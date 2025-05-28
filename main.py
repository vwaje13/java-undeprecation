import subprocess
import os
import shutil
import google.generativeai as genai
from dotenv import load_dotenv
import vertexai
from vertexai.generative_models import GenerativeModel, GenerationConfig
import glob

# --- Configuration ---
# Load environment variables from .env file
load_dotenv()

# Input and output directories
INPUT_DIR = "./input"
OUTPUT_DIR = "./output"

# Fixed Gemini model
GEMINI_MODEL = "gemini-2.0-flash-001"

# Configure Vertex AI for Gemini
try:
    # Get API keys from environment variables
    project_id = os.getenv("GCP_PROJECT_ID")
    credentials_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    
    if not project_id:
        raise ValueError("GCP_PROJECT_ID not found in environment variables")
    if not credentials_path:
        raise ValueError("GOOGLE_APPLICATION_CREDENTIALS not found in environment variables")
    
    # Initialize Vertex AI with the project and credentials
    vertexai.init(project=project_id, location="us-central1")
    print(f"Successfully initialized Vertex AI with project: {project_id}")
    
except Exception as e:
    print(f"Error initializing Vertex AI: {e}")
    print("Please ensure the GCP_PROJECT_ID and GOOGLE_APPLICATION_CREDENTIALS environment variables are set correctly in the .env file.")
    exit(1)

# --- Helper Functions ---

def run_command(command, working_dir="."):
    """Executes a shell command and returns its output and error."""
    print(f"Executing: {' '.join(command)} in {working_dir}")
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=False, cwd=working_dir)
        if result.returncode != 0:
            print(f"Warning: Command returned non-zero exit code {result.returncode}")
            print(f"Stdout:\n{result.stdout}")
            print(f"Stderr:\n{result.stderr}")
        return result.stdout, result.stderr, result.returncode
    except FileNotFoundError:
        print(f"Error: Command not found: {command[0]}. Please ensure it's in your PATH.")
        return None, f"Command not found: {command[0]}", 1
    except Exception as e:
        print(f"An error occurred while running command {' '.join(command)}: {e}")
        return None, str(e), 1

def analyze_with_jdeprscan(java_file_path, release_version):
    """
    Runs jdeprscan to find deprecated API usage.
    release_version: The Java SE release version to check against (e.g., "17").
    """
    print(f"Analyzing {java_file_path} for deprecated APIs against Java {release_version}...")
    command = ["jdeprscan", "--release", str(release_version), java_file_path]
    stdout, stderr, returncode = run_command(command)

    if returncode != 0 and "java.lang.module.FindException: Module java.se not found" in stderr:
        print(f"Warning: 'jdeprscan --release {release_version}' failed to find java.se module.")
        print("This might happen if the JDK used by jdeprscan doesn't have full support for that --release flag.")
        print("Trying without --release flag, which will use the jdeprscan's own JDK version for analysis.")
        command = ["jdeprscan", java_file_path] # Fallback
        stdout, stderr, returncode = run_command(command)

    if stderr and "No class given" not in stderr and "No deprecated API" not in stdout : # jdeprscan outputs "No class given..." to stderr if file has no class
        print(f"jdeprscan errors/warnings:\n{stderr}")
    if "No deprecated API" in stdout:
        print("jdeprscan found no deprecated API usage.")
        return ""
    return stdout

def get_llm_suggestion(java_code, deprecated_list, target_jdk_version):
    """
    Sends the Java code and list of deprecated items to Gemini for an updated version.
    """
    print(f"Asking Gemini to update code for JDK {target_jdk_version}...")
    
    # Create the prompt for Gemini
    prompt = f"""
You are a Java expert specializing in migrating code from older Java versions to newer ones.
The following Java code uses some APIs and/or methods or functions that are deprecated or have better alternatives in newer Java versions.
Your task is to rewrite the code to be compatible with Java {target_jdk_version}, replacing deprecated elements with their modern equivalents.
Ensure the core logic and functionality of the code remain unchanged. do NOT refactor or change the code if it is not necessary for this use case.
Provide only the complete updated Java code block, without any explanations or markdown formatting outside the code block.

Here is the list of deprecated items found by jdeprscan (this might be empty or incomplete, so also rely on your general knowledge of Java {target_jdk_version} changes):
<deprecated_list>
{deprecated_list}
</deprecated_list>

Original Java code:
```java
{java_code}
```

Updated Java code for Java {target_jdk_version}:
"""
    
    try:
        # Get the Gemini model from Vertex AI
        model = GenerativeModel(GEMINI_MODEL)
        
        # Generate content with Gemini
        response = model.generate_content(
            prompt,
            generation_config=GenerationConfig(
                temperature=0.2,  # Low temperature for more deterministic results
                max_output_tokens=8192,  # Allow longer responses for complete code
                top_p=0.95,
            )
        )
        
        # Extract the response text
        updated_code = response.text.strip()
        
        # Post-process to remove markdown code block fences if Gemini includes them
        if updated_code.startswith("```java"):
            updated_code = updated_code[len("```java"):].strip()
        if updated_code.endswith("```"):
            updated_code = updated_code[:-len("```")].strip()
            
        return updated_code
    except Exception as e:
        print(f"Error calling Gemini API: {e}")
        return None

def compile_java(java_file_path, output_dir):
    """Compiles a Java file."""
    print(f"Compiling {java_file_path}...")
    os.makedirs(output_dir, exist_ok=True)
    # Ensure javac is for the target JDK. If multiple JDKs, might need to specify full path.
    command = ["javac", "-d", output_dir, java_file_path]
    stdout, stderr, returncode = run_command(command, working_dir=os.path.dirname(java_file_path) or ".")
    if returncode == 0:
        print(f"Compilation successful. Class files in {output_dir}")
        return True
    else:
        print(f"Compilation failed for {java_file_path}.")
        print(f"Stdout:\n{stdout}")
        print(f"Stderr:\n{stderr}")
        return False

def run_java(class_name, classpath_dir, output_dir):
    """Runs a compiled Java class and captures its output."""
    print(f"Running {class_name} from {classpath_dir}...")
    # Ensure java is for the target JDK.
    command = ["java", "-cp", classpath_dir, class_name]
    # Capture output to a file to avoid issues with large outputs / subprocess buffering
    output_file_path = os.path.join(output_dir, f"{class_name}_run_output.txt")

    try:
        with open(output_file_path, "w") as outfile:
            result = subprocess.run(command, stdout=outfile, stderr=subprocess.PIPE, text=True, check=False, cwd=output_dir)

        with open(output_file_path, "r") as outfile:
            stdout_content = outfile.read()

        if result.returncode == 0:
            print(f"Execution successful. Output captured in {output_file_path}")
            return stdout_content, result.stderr, result.returncode
        else:
            print(f"Execution failed for {class_name}.")
            print(f"Stdout (from file):\n{stdout_content}")
            print(f"Stderr:\n{result.stderr}")
            return stdout_content, result.stderr, result.returncode

    except FileNotFoundError:
        print(f"Error: java command not found. Please ensure it's in your PATH.")
        return None, "java command not found", 1
    except Exception as e:
        print(f"An error occurred while running java for {class_name}: {e}")
        return None, str(e), 1

def get_class_name_from_source(java_code):
    """Attempts to find the main public class name from Java source code."""
    # This is a simplistic approach. Robust parsing would require a Java parser.
    import re
    match = re.search(r"public\s+class\s+([A-Za-z0-9_]+)", java_code)
    if match:
        return match.group(1)
    match = re.search(r"class\s+([A-Za-z0-9_]+)", java_code) # Fallback for non-public main class
    if match:
        return match.group(1)
    return None

def list_java_files():
    """List all Java files in the input directory."""
    java_files = glob.glob(os.path.join(INPUT_DIR, "*.java"))
    return [os.path.basename(f) for f in java_files]

def get_user_selection(options, prompt_message):
    """Get user selection from a list of options."""
    print("\n" + prompt_message)
    for i, option in enumerate(options, 1):
        print(f"{i}. {option}")
    
    while True:
        try:
            choice = int(input("\nEnter your choice (number): "))
            if 1 <= choice <= len(options):
                return options[choice-1]
            else:
                print(f"Please enter a number between 1 and {len(options)}")
        except ValueError:
            print("Please enter a valid number")

def main():
    # Ensure input and output directories exist
    os.makedirs(INPUT_DIR, exist_ok=True)
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    print("\n===== Java Deprecation Updater =====\n")
    
    # Check if there are Java files in the input directory
    java_files = list_java_files()
    if not java_files:
        print(f"No Java files found in {INPUT_DIR} directory.")
        print(f"Please place your Java files in the {INPUT_DIR} directory and run the script again.")
        return
    
    # User selects a Java file
    selected_file = get_user_selection(
        java_files, 
        "Select a Java file to process:"
    )
    input_file_path = os.path.join(INPUT_DIR, selected_file)
    
    # User selects JDK versions
    jdk_versions = ["16", "20", "21"]
    
    old_jdk_version = get_user_selection(
        jdk_versions, 
        "Select the OLD JDK version (the version your code is currently using):"
    )
    
    # Filter versions newer than the old JDK
    newer_versions = [v for v in jdk_versions if int(v) > int(old_jdk_version)]
    if not newer_versions:
        print("There are no newer JDK versions available.")
        return
    
    new_jdk_version = get_user_selection(
        newer_versions, 
        "Select the NEW JDK version (the version you want to upgrade to):"
    )
    
    print(f"\nProcessing {selected_file}")
    print(f"Upgrading from JDK {old_jdk_version} to JDK {new_jdk_version}")
    print(f"Using Gemini model: {GEMINI_MODEL}")
    
    # Extract file information
    base_name = os.path.basename(input_file_path)
    file_name_no_ext = os.path.splitext(base_name)[0]

    # Define output paths
    updated_java_file_path = os.path.join(OUTPUT_DIR, f"{file_name_no_ext}.java")

    # 1. Read input Java file
    print(f"\nReading input file: {input_file_path}")
    try:
        with open(input_file_path, "r") as f:
            original_java_code = f.read()
    except Exception as e:
        print(f"Error reading input file {input_file_path}: {e}")
        return

    # Attempt to get class name for running later
    original_class_name = get_class_name_from_source(original_java_code)
    if not original_class_name:
        print("Warning: Could not determine main class name from original source. Execution might fail or require manual class name.")
        # Fallback to filename if class name cannot be parsed (common for simple single-file examples)
        original_class_name = file_name_no_ext
        print(f"Assuming class name is: {original_class_name} based on filename.")

    # 2. Use jdeprscan to get deprecated usages
    print("\n--- Step 2: Analyze with jdeprscan ---")
    deprecated_list = analyze_with_jdeprscan(input_file_path, str(old_jdk_version))
    print(f"jdeprscan output:\n{deprecated_list if deprecated_list else 'No specific deprecated APIs listed by jdeprscan for source file.'}")

    # 3. Call Gemini with prompt + code + deprecated items
    print("\n--- Step 3: Get Gemini suggestion for code update ---")
    updated_java_code = get_llm_suggestion(original_java_code, deprecated_list, str(new_jdk_version))

    if not updated_java_code:
        print("Gemini did not return updated code. Exiting.")
        return

    # 4. Save new .java file
    print(f"\n--- Step 4: Save updated Java code ---")
    try:
        with open(updated_java_file_path, "w") as f:
            f.write(updated_java_code)
        print(f"Updated code saved to: {updated_java_file_path}")
    except Exception as e:
        print(f"Error writing updated Java file {updated_java_file_path}: {e}")
        return

    print("\nProcess finished.")
    print(f"\nUpdated Java file is saved at: {updated_java_file_path}")

if __name__ == "__main__":
    main() 