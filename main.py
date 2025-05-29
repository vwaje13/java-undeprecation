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
    except Exception as e:
        print(f"Error writing files: {e}")

if __name__ == "__main__":
    main() 