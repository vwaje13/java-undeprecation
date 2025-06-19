import subprocess
import os
import re
import glob
import shutil
import tempfile
import xml.etree.ElementTree as ET
from dotenv import load_dotenv
import vertexai
from vertexai.generative_models import GenerativeModel, GenerationConfig

# Load environment variables and set up directories
load_dotenv()
INPUT_DIR = "./input"
OUTPUT_DIR = "./output"
GEMINI_MODEL = "gemini-2.0-flash-001"

# Initialize Vertex AI
try:
    project_id = os.getenv("GCP_PROJECT_ID")
    credentials_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    
    if not project_id or not credentials_path:
        raise ValueError("Missing GCP_PROJECT_ID or GOOGLE_APPLICATION_CREDENTIALS in environment variables")
    
    vertexai.init(project=project_id, location="us-central1")
    print(f"Successfully initialized Vertex AI with project: {project_id}")
except Exception as e:
    print(f"Error initializing Vertex AI: {e}")
    print("Please ensure GCP_PROJECT_ID and GOOGLE_APPLICATION_CREDENTIALS are set correctly in .env file.")
    exit(1)

# Maven POM Template
POM_TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example.modernizer</groupId>
    <artifactId>modernizer-check</artifactId>
    <version>1.0-SNAPSHOT</version>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>{java_version}</maven.compiler.source>
        <maven.compiler.target>{java_version}</maven.compiler.target>
        <modernizer.javaVersion>{java_version}</modernizer.javaVersion>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${{maven.compiler.source}}</source>
                    <target>${{maven.compiler.target}}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.gaul</groupId>
                <artifactId>modernizer-maven-plugin</artifactId>
                <version>2.7.0</version>
                <configuration>
                    <javaVersion>${{modernizer.javaVersion}}</javaVersion>
                    <failOnViolations>false</failOnViolations>
                </configuration>
                <executions>
                    <execution>
                        <id>modernizer</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>modernizer</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
"""

def run_command(command, working_dir="."):
    """Executes a shell command and returns stdout, stderr, and return code."""
    print(f"Executing: {' '.join(command)} in {working_dir}")
    try:
        result = subprocess.run(command, capture_output=True, text=True, check=False, cwd=working_dir)
        if result.returncode != 0:
            print(f"Warning: Command returned non-zero exit code {result.returncode}")
        return result.stdout, result.stderr, result.returncode
    except Exception as e:
        print(f"Error running command {' '.join(command)}: {e}")
        return None, str(e), 1

def get_package_path(java_code):
    """Extracts package path from Java source code."""
    package_match = re.search(r"^\s*package\s+([\w.]+);", java_code, re.MULTILINE)
    return package_match.group(1).replace('.', '/') if package_match else ""

def analyze_with_modernizer(java_file_path, target_jdk_version):
    """Runs modernizer-maven-plugin to find API usage needing modernization."""
    print(f"Analyzing {java_file_path} with Modernizer for Java {target_jdk_version}...")
    
    try:
        with open(java_file_path, 'r') as f:
            java_code = f.read()
    except Exception as e:
        print(f"Error reading Java file: {e}")
        return f"Error reading Java file: {e}"

    temp_dir = tempfile.mkdtemp(prefix="modernizer_")
    print(f"Created temp directory: {temp_dir}")

    try:
        # Set up Maven project structure
        src_main_java_path = os.path.join(temp_dir, "src", "main", "java")
        package_path = get_package_path(java_code)
        target_dir = os.path.join(src_main_java_path, package_path) if package_path else src_main_java_path
        os.makedirs(target_dir, exist_ok=True)
        
        # Copy Java file and create POM
        file_name = os.path.basename(java_file_path)
        shutil.copy(java_file_path, os.path.join(target_dir, file_name))
        with open(os.path.join(temp_dir, "pom.xml"), "w") as f:
            f.write(POM_TEMPLATE.format(java_version=target_jdk_version))

        # Run Maven
        stdout, stderr, returncode = run_command(["mvn", "-B", "clean", "verify"], working_dir=temp_dir)
        
        # Check for report or parse output for errors
        report_path = os.path.join(temp_dir, "target", "modernizer-report.xml")
        if os.path.exists(report_path):
            # Parse XML report
            tree = ET.parse(report_path)
            violations = []
            for violation in tree.findall(".//violation"):
                name = violation.find("name").text
                comment = violation.find("comment").text
                locations = []
                for loc in violation.findall(".//location"):
                    line = loc.get("lineNumber")
                    source_file = loc.get("sourceFile")
                    locations.append(f"at {source_file}:L{line}")
                violations.append(f"- {name}: {comment} ({', '.join(locations)})")
            
            return "Modernizer found no violations." if not violations else "Modernizer found the following issues:\n" + "\n".join(violations)
        else:
            # Try to extract errors from Maven output
            modernizer_errors = []
            for line in stdout.splitlines():
                if "[ERROR]" in line and "Prefer" in line:
                    error_parts = line.split(": ", 1)
                    if len(error_parts) > 1:
                        file_line = error_parts[0].split("/")[-1]
                        message = error_parts[1]
                        modernizer_errors.append(f"- {message} (at {file_line})")
            
            if modernizer_errors:
                return "Modernizer found the following issues:\n" + "\n".join(modernizer_errors)
            elif "No violations found" in stdout:
                return "Modernizer found no violations."
            else:
                return "Modernizer report not found. Check if Modernizer ran correctly."
    except Exception as e:
        return f"Error during Modernizer analysis: {e}"
    finally:
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)

def get_llm_suggestion(java_code, modernizer_findings, target_jdk_version):
    """Gets suggestions from Gemini for code modernization."""
    print(f"Asking Gemini to update code for JDK {target_jdk_version}...")
    
    prompt = f"""
                You are a Java expert specializing in migrating code from older Java versions to newer ones, incorporating suggestions from static analysis tools.
                The following Java code needs to be updated to be compatible with Java {target_jdk_version}.
                Modernizer static analysis tool has found the following potential issues or areas for improvement:
                <modernizer_findings>
                {modernizer_findings}
                </modernizer_findings>

                Your task is to:
                1. Review the original Java code and the Modernizer findings.
                2. Rewrite the code to be compatible with Java {target_jdk_version}, addressing the issues highlighted by Modernizer and replacing any other deprecated elements with their modern equivalents.
                3. Ensure the core logic and functionality of the code remain unchanged. Do NOT refactor or change the code if it is not strictly necessary for modernization or addressing Modernizer's points.
                4. Provide a brief summary of the changes made, explaining what was deprecated/flagged and what it was replaced with, specifically referencing Modernizer's findings where applicable.

                Please format your response as follows:

                <change_summary>
                [Your summary of changes here, referencing Modernizer findings]
                </change_summary>

                <updated_code>
                ```java
                [Your updated Java code here]
                ```
                </updated_code>

                Original Java code:
                ```java
                {java_code}
                ```
            """
    
    try:
        model = GenerativeModel(GEMINI_MODEL)
        response = model.generate_content(
            prompt,
            generation_config=GenerationConfig(
                temperature=0.2,
                max_output_tokens=8192,
                top_p=0.95,
            )
        )
        response_text = response.text.strip()
        
        # Extract summary
        summary_match = re.search(r"<change_summary>(.*?)</change_summary>", response_text, re.DOTALL)
        change_summary = summary_match.group(1).strip() if summary_match else "LLM did not provide a structured summary."

        # Extract code
        code_match = re.search(r"<updated_code>\s*```java\n(.*?)\n```\s*</updated_code>", response_text, re.DOTALL)
        if code_match:
            updated_code = code_match.group(1).strip()
        else:
            # Try alternative code block formats
            code_block_match = re.search(r"```java\n(.*?)\n```", response_text, re.DOTALL)
            if code_block_match:
                updated_code = code_block_match.group(1).strip()
            else:
                # Last resort cleanup
                cleaned_response = re.sub(r"<change_summary>.*?</change_summary>", "", response_text, flags=re.DOTALL).strip()
                cleaned_response = re.sub(r"<updated_code>.*?</updated_code>", "", cleaned_response, flags=re.DOTALL).strip()
                
                if "```java" in cleaned_response:
                    updated_code = cleaned_response.split("```java", 1)[-1].split("```", 1)[0].strip()
                else:
                    updated_code = None
        
        return updated_code, change_summary
        
    except Exception as e:
        print(f"Error calling Gemini API: {e}")
        return None, f"Error calling Gemini API: {e}"

def get_class_name_from_source(java_code):
    """Extracts the main class name from Java source code."""
    match = re.search(r"public\s+class\s+([A-Za-z0-9_]+)", java_code) or re.search(r"class\s+([A-Za-z0-9_]+)", java_code)
    return match.group(1) if match else None

def get_user_selection(options, prompt_message):
    """Gets user selection from a list of options."""
    print("\n" + prompt_message)
    for i, option in enumerate(options, 1):
        print(f"{i}. {option}")
    
    while True:
        try:
            choice = int(input("\nEnter your choice (number): "))
            if 1 <= choice <= len(options):
                return options[choice-1]
            print(f"Please enter a number between 1 and {len(options)}")
        except ValueError:
            print("Please enter a valid number")

def compile_and_run_java(java_file_path, java_code_content, version_label=""):
    """Compiles and runs a single Java file in a temporary directory, returning its stdout or an error message."""
    print(f"Processing {version_label} version: {java_file_path}")
    
    class_name = get_class_name_from_source(java_code_content)
    if not class_name:
        return None, f"Could not extract class name from {java_file_path}."

    temp_dir = tempfile.mkdtemp(prefix=f"java_run_{version_label}_")
    # print(f"Created temporary directory for {version_label} Java execution: {temp_dir}")

    try:
        base_name = os.path.basename(java_file_path) # e.g., MyClass.java
        package_as_path = get_package_path(java_code_content) # e.g., com/example or ""
        
        # Path to the .java file within the temp_dir, including package structure
        # e.g., temp_dir/com/example/MyClass.java or temp_dir/MyClass.java
        java_file_rel_path_in_temp = os.path.join(package_as_path, base_name)
        full_java_file_path_in_temp = os.path.join(temp_dir, java_file_rel_path_in_temp)

        # Create package directories if they exist
        if package_as_path:
            os.makedirs(os.path.dirname(full_java_file_path_in_temp), exist_ok=True)
        
        with open(full_java_file_path_in_temp, "w") as f:
            f.write(java_code_content)

        # Compile: javac is run from temp_dir, refers to file by its relative path within temp_dir
        # e.g., javac com/example/MyClass.java or javac MyClass.java
        print(f"Compiling {java_file_rel_path_in_temp} in {temp_dir}...")
        compile_cmd = ["javac", java_file_rel_path_in_temp]
        stdout_compile, stderr_compile, returncode_compile = run_command(compile_cmd, working_dir=temp_dir)
        
        if returncode_compile != 0:
            error_msg = f"Compilation failed for {version_label} code ({base_name}).\\nStdout: {stdout_compile}\\nStderr: {stderr_compile}"
            print(error_msg)
            return None, error_msg
        print(f"Compilation successful for {version_label} code.")

        # Run: java is run from temp_dir, refers to class by FQN
        # e.g., java com.example.MyClass or java MyClass
        package_name_dotted = package_as_path.replace('/', '.') # com.example or ""
        fully_qualified_class_name = f"{package_name_dotted}.{class_name}" if package_name_dotted else class_name
        
        print(f"Running {fully_qualified_class_name} from {temp_dir}...")
        run_cmd = ["java", fully_qualified_class_name]
        # Classpath is implicitly temp_dir because we are running `java` from temp_dir
        stdout_run, stderr_run, returncode_run = run_command(run_cmd, working_dir=temp_dir)
        
        if returncode_run != 0:
            error_msg = f"Execution failed for {version_label} code ({fully_qualified_class_name}).\\nStdout: {stdout_run}\\nStderr: {stderr_run}"
            print(error_msg)
            # Return stdout_run as it might contain partial output before an error
            return stdout_run, error_msg 
        
        print(f"Execution successful for {version_label} code.")
        return stdout_run, None

    except Exception as e:
        print(f"Exception in compile_and_run_java for {version_label}: {e}")
        return None, str(e)
    finally:
        if os.path.exists(temp_dir):
            # print(f"Cleaning up temporary directory: {temp_dir}")
            shutil.rmtree(temp_dir)

def compute_line_differences(original_output, updated_output):
    """Computes and returns line-by-line differences between two outputs."""
    original_lines = original_output.splitlines()
    updated_lines = updated_output.splitlines()
    
    differences = []
    max_lines = max(len(original_lines), len(updated_lines))
    
    for i in range(max_lines):
        original_line = original_lines[i] if i < len(original_lines) else None
        updated_line = updated_lines[i] if i < len(updated_lines) else None
        
        if original_line != updated_line:
            differences.append({
                'line_number': i + 1,
                'original': original_line,
                'updated': updated_line
            })
    
    return differences

def test_java_versions(original_file_path, original_java_code, updated_file_path, updated_java_code):
    """Compiles, runs, and compares outputs of original and updated Java code."""
    print("\n--- Testing Original Code ---")
    original_output, original_error = compile_and_run_java(original_file_path, original_java_code, "original")

    if original_error and original_output is None : # If error completely prevented output
        print(f"Error processing original code: {original_error}")
        print("Cannot compare outputs due to critical error with original code.")
        return
    elif original_error: # Error occurred but there might be partial output
         print(f"Note: Original code execution had errors: {original_error}")


    print("\n--- Testing Updated Code ---")
    updated_output, updated_error = compile_and_run_java(updated_file_path, updated_java_code, "updated")

    if updated_error and updated_output is None: # If error completely prevented output
        print(f"Error processing updated code: {updated_error}")
        print("Cannot compare outputs due to critical error with updated code.")
        return
    elif updated_error: # Error occurred but there might be partial output
        print(f"Note: Updated code execution had errors: {updated_error}")


    print("\n--- Comparison of Outputs ---")
    # Ensure outputs are not None before comparison, default to empty string if None (though errors should be caught above)
    original_output_for_compare = original_output if original_output is not None else ""
    updated_output_for_compare = updated_output if updated_output is not None else ""

    if original_output_for_compare == updated_output_for_compare:
        print("SUCCESS: Outputs of original and updated code are identical.")
        if original_error or updated_error:
            print("However, note that one or both versions encountered non-critical errors during execution (check logs above).")
    else:
        print("DIFFERENCE DETECTED: Outputs of original and updated code differ.")
        
        # Compute and display line differences
        differences = compute_line_differences(original_output_for_compare, updated_output_for_compare)
        print("\nLine-by-line differences:")
        for diff in differences:
            print(f"\nLine {diff['line_number']}:")
            print(f"  Original: {diff['original']}")
            print(f"  Updated:  {diff['updated']}")
        
        print("\n--- Original Code Output ---")
        print(original_output_for_compare if original_output_for_compare.strip() else "[No Stdout]")
        print("\n--- Updated Code Output ---")
        print(updated_output_for_compare if updated_output_for_compare.strip() else "[No Stdout]")
        if original_error or updated_error:
            print("\nNote: One or both versions also encountered errors during execution (check logs above).")

def main():
    # Create directories
    os.makedirs(INPUT_DIR, exist_ok=True)
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    print("\n===== Java Modernization Updater =====\n")
    
    # Get Java files
    java_files = [os.path.basename(f) for f in glob.glob(os.path.join(INPUT_DIR, "*.java"))]
    if not java_files:
        print(f"No Java files found in {INPUT_DIR} directory.")
        print(f"Please place your Java files in the {INPUT_DIR} directory and run the script again.")
        return
    
    # Get user selections
    selected_file = get_user_selection(java_files, "Select a Java file to process:")
    target_jdk_version = get_user_selection(["11", "17", "21"], "Select the target JDK version to modernize to:")
    
    print(f"\nProcessing {selected_file} for JDK {target_jdk_version} using {GEMINI_MODEL}")
    
    # Set up file paths
    input_file_path = os.path.join(INPUT_DIR, selected_file)
    file_name_no_ext = os.path.splitext(selected_file)[0]
    updated_java_file_path = os.path.join(OUTPUT_DIR, f"{file_name_no_ext}.java")
    summary_file_path = os.path.join(OUTPUT_DIR, f"{file_name_no_ext}_modernizer_changes.txt")

    # Read input file
    try:
        with open(input_file_path, "r") as f:
            original_java_code = f.read()
    except Exception as e:
        print(f"Error reading input file: {e}")
        return

    # Step 1: Analyze with Modernizer
    print("\n--- Step 1: Analyze with Modernizer ---")
    modernizer_findings = analyze_with_modernizer(input_file_path, target_jdk_version)
    print(f"Modernizer Analysis Output:\n{modernizer_findings}")

    if modernizer_findings is None or "Modernizer execution failed" in modernizer_findings or "Error during Modernizer analysis" in modernizer_findings:
        print("\nModernizer analysis failed. Cannot proceed to LLM suggestion.")
        with open(summary_file_path, "w") as f:
            f.write(f"Java Code Modernization Attempt Summary\n")
            f.write(f"=====================================\n\n")
            f.write(f"Original file: {input_file_path}\n")
            f.write(f"Target JDK: {target_jdk_version}\n\n")
            f.write(f"Modernizer Analysis Failed:\n")
            f.write(str(modernizer_findings))
        print(f"Modernizer log saved to: {summary_file_path}")
        return

    # Step 2: Get LLM suggestion
    print("\n--- Step 2: Get Gemini suggestion for code update ---")
    updated_java_code, llm_summary = get_llm_suggestion(original_java_code, modernizer_findings, target_jdk_version)

    if not updated_java_code:
        print("Gemini did not return updated code. Exiting.")
        with open(summary_file_path, "w") as f:
            f.write(f"Java Code Modernization Attempt Summary\n")
            f.write(f"=====================================\n\n")
            f.write(f"Original file: {input_file_path}\n")
            f.write(f"Target JDK: {target_jdk_version}\n\n")
            f.write(f"Modernizer Analysis Output:\n{modernizer_findings}\n\n")
            f.write(f"LLM Processing Output (Code generation failed):\n{llm_summary}")
        print(f"Attempt details saved to: {summary_file_path}")
        return

    print("\n--- LLM Summary of Changes ---")
    print(llm_summary)

    # Step 3: Save updated code and summary
    print(f"\n--- Step 3: Save updated Java code and LLM summary ---")
    try:
        with open(updated_java_file_path, "w") as f:
            f.write(updated_java_code)
        
        with open(summary_file_path, "w") as f:
            f.write(f"Java Code Modernization Summary (Modernizer + LLM)\n")
            f.write(f"==================================================\n\n")
            f.write(f"Original file: {input_file_path}\n")
            f.write(f"Updated file: {updated_java_file_path}\n")
            f.write(f"Target JDK: {target_jdk_version}\n\n")
            f.write("Modernizer Analysis Findings:\n")
            f.write("----------------------------\n")
            f.write(modernizer_findings)
            f.write("\n\nLLM Generated Summary of Changes:\n")
            f.write("----------------------------------\n")
            f.write(llm_summary)
        
        print(f"Updated Java file saved to: {updated_java_file_path}")
        print(f"Modernizer findings and LLM summary saved to: {summary_file_path}")

        # Ask user if they want to run and compare code versions
        run_tests_choice = input("\nDo you want to compile and run both the original and updated code to check for output differences? (y/n): ").strip().lower()
        if run_tests_choice == 'y':
            print("\n--- Step 4: Test Execution Differences ---")
            test_java_versions(input_file_path, original_java_code, 
                               updated_java_file_path, updated_java_code)

    except Exception as e:
        print(f"Error writing files or testing: {e}")

if __name__ == "__main__":
    main() 