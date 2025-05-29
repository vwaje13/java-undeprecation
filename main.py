import subprocess
import os
import shutil
import google.generativeai as genai
from dotenv import load_dotenv
import vertexai
from vertexai.generative_models import GenerativeModel, GenerationConfig
import glob
import difflib
import html

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
    
    # First, compile the Java file
    java_dir = os.path.dirname(java_file_path)
    java_filename = os.path.basename(java_file_path)
    class_name = os.path.splitext(java_filename)[0]
    
    # Compile command
    compile_cmd = ["javac", java_filename]
    compile_stdout, compile_stderr, compile_ret = run_command(compile_cmd, working_dir=java_dir)
    
    if compile_ret != 0:
        print(f"Warning: Failed to compile {java_file_path} for jdeprscan analysis")
        print(f"Compilation errors: {compile_stderr}")
        return ""
    
    # Now run jdeprscan on the compiled class
    command = ["jdeprscan", "--class-path", ".", "--release", str(release_version), class_name]
    stdout, stderr, returncode = run_command(command, working_dir=java_dir)

    if returncode != 0:
        print(f"Warning: jdeprscan returned error code {returncode}")
        if stderr:
            print(f"jdeprscan errors/warnings:\n{stderr}")
        
        # Fallback to running jdeprscan without the --release option
        print(f"Trying jdeprscan without --release flag...")
        command = ["jdeprscan", "--class-path", ".", class_name] 
        stdout, stderr, returncode = run_command(command, working_dir=java_dir)
    
    if returncode != 0 and stderr:
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

def analyze_api_migrations(original_code, updated_code):
    """
    Analyzes the code changes to identify and summarize the types of API migrations that occurred.
    Returns a short description of the migrations.
    """
    # Common deprecated APIs and their modern replacements
    migration_patterns = [
        # Collections
        ("Vector", "ArrayList"),
        ("Hashtable", "HashMap"),
        ("Stack", "Deque"),
        ("Enumeration", "Iterator"),
        ("StringTokenizer", "String.split"),
        # Date/Time
        ("new Date(", "LocalDate"),
        ("SimpleDateFormat", "DateTimeFormatter"),
        # File IO
        ("new FileInputStream", "Files.newInputStream"),
        ("new FileOutputStream", "Files.newOutputStream"),
        # Thread methods
        ("Thread.stop", "interrupt mechanism"),
        # Deprecated methods
        ("addElement", "add"),
        ("elements()", "iterator()"),
    ]
    
    migrations_found = []
    
    for old_api, new_api in migration_patterns:
        if old_api in original_code and old_api not in updated_code:
            if new_api in updated_code and new_api not in original_code:
                migrations_found.append(f"{old_api} → {new_api}")
            else:
                migrations_found.append(f"Removed {old_api}")
    
    # Look for import changes
    original_imports = [line for line in original_code.splitlines() if line.strip().startswith("import ")]
    updated_imports = [line for line in updated_code.splitlines() if line.strip().startswith("import ")]
    
    new_imports = set(updated_imports) - set(original_imports)
    removed_imports = set(original_imports) - set(updated_imports)
    
    if new_imports or removed_imports:
        imports_summary = []
        if len(removed_imports) > 0 and len(new_imports) > 0:
            imports_summary.append(f"Updated imports ({len(removed_imports)} removed, {len(new_imports)} added)")
        
        # Look for specific important package migrations
        if any("java.time" in imp for imp in new_imports):
            imports_summary.append("Added modern java.time package")
        if any("java.nio" in imp for imp in new_imports):
            imports_summary.append("Added modern java.nio file operations")
        if any("java.util.concurrent" in imp for imp in new_imports):
            imports_summary.append("Added concurrent collections")
            
        if imports_summary:
            migrations_found.extend(imports_summary)
    
    if not migrations_found:
        return "No significant API migrations detected."
    
    return "API Migrations: " + ", ".join(migrations_found)

def generate_code_diff_summary(original_code, updated_code):
    """
    Generates a concise summary of changes between original and updated code.
    Returns a string with the summary of changes.
    """
    # Generate API migration summary
    migration_summary = []
    
    # Common deprecated APIs and their modern replacements
    migration_patterns = [
        # Collections
        ("Vector", "ArrayList"),
        ("Hashtable", "HashMap"),
        ("Stack", "Deque"),
        ("Enumeration", "Iterator"),
        ("StringTokenizer", "String.split"),
        # Date/Time
        ("new Date(", "LocalDate"),
        ("SimpleDateFormat", "DateTimeFormatter"),
        # File IO
        ("new FileInputStream", "Files.newInputStream"),
        ("new FileOutputStream", "Files.newOutputStream"),
        # Thread methods
        ("Thread.stop", "interrupt mechanism"),
        # Deprecated methods
        ("addElement", "add"),
        ("elements()", "iterator()"),
    ]
    
    for old_api, new_api in migration_patterns:
        if old_api in original_code and old_api not in updated_code:
            if new_api in updated_code and new_api not in original_code:
                migration_summary.append(f"{old_api} → {new_api}")
    
    # Look for import changes
    original_imports = [line for line in original_code.splitlines() if line.strip().startswith("import ")]
    updated_imports = [line for line in updated_code.splitlines() if line.strip().startswith("import ")]
    
    new_imports = set(updated_imports) - set(original_imports)
    if any("java.time" in imp for imp in new_imports):
        migration_summary.append("Added modern java.time package")
    if any("java.nio" in imp for imp in new_imports):
        migration_summary.append("Added modern java.nio file operations")
    
    original_lines = original_code.splitlines()
    updated_lines = updated_code.splitlines()
    
    # Use difflib to compute the differences
    diff = list(difflib.unified_diff(
        original_lines, 
        updated_lines,
        n=2,  # Context lines
        lineterm=''
    ))
    
    # Skip the first two lines which are just headers
    if len(diff) > 2:
        diff = diff[2:]
    
    # Create a summary of changes
    changes = []
    current_section = None
    line_number = 0
    
    for line in diff:
        if line.startswith('@@'):
            # Parse the line numbers from the @@ line
            # Format is @@ -original_start,original_count +updated_start,updated_count @@
            try:
                line_info = line.split('@@')[1].strip()
                original_info = line_info.split(' ')[0]
                if ',' in original_info:
                    line_number = int(original_info.split(',')[0][1:])  # Remove the '-' prefix
                else:
                    line_number = int(original_info[1:])  # Just the line number without count
                current_section = []
                changes.append((line_number, current_section))
            except (IndexError, ValueError):
                continue
        elif current_section is not None:
            if line.startswith('-'):
                current_section.append(("removed", line_number, line[1:].strip()))
                line_number += 1
            elif line.startswith('+'):
                current_section.append(("added", line_number, line[1:].strip()))
            elif line.startswith(' '):
                line_number += 1
    
    # Format the changes into a readable summary
    summary_lines = ["Summary of Changes:"]
    
    # Add API migration summary if available
    if migration_summary:
        summary_lines.append("API Migrations: " + ", ".join(migration_summary))
        summary_lines.append("")  # Add blank line
    
    for start_line, section in changes:
        if not section:  # Skip empty sections
            continue
            
        # Group related changes
        imports_changed = False
        api_changes = []
        other_changes = []
        
        for change_type, line_num, content in section:
            if "import " in content:
                imports_changed = True
            elif change_type in ("removed", "added"):
                # Look for API method changes
                if any(api in content for api in ["new ", ".get", ".add", ".create", ".format", "File", "Date", "Vector", "Hashtable", "Stack", "Enumeration", "StringTokenizer"]):
                    api_changes.append((change_type, line_num, content))
                else:
                    other_changes.append((change_type, line_num, content))
        
        # Add a section header with context
        context_line = f"Lines {start_line}-{start_line + len(section)}:"
        summary_lines.append(context_line)
        
        # Report import changes
        if imports_changed:
            summary_lines.append("  • Updated import statements")
            
        # Report API changes
        if api_changes:
            summary_lines.append("  • API changes:")
            for change_type, line_num, content in api_changes:
                change_symbol = "-" if change_type == "removed" else "+"
                summary_lines.append(f"    {change_symbol} Line {line_num}: {content}")
        
        # Report other changes
        if other_changes:
            summary_lines.append("  • Other changes:")
            for change_type, line_num, content in other_changes:
                change_symbol = "-" if change_type == "removed" else "+"
                summary_lines.append(f"    {change_symbol} Line {line_num}: {content}")
    
    if len(summary_lines) == 1:  # Only contains the header
        return "No significant changes detected."
    
    return "\n".join(summary_lines)

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

def generate_html_diff(original_code, updated_code, file_name, old_version, new_version):
    """
    Generate an HTML file with colorized side-by-side diff view.
    """
    original_lines = original_code.splitlines()
    updated_lines = updated_code.splitlines()
    
    # Create the HTML content
    html_content = [
        '<!DOCTYPE html>',
        '<html>',
        '<head>',
        f'<title>Java Migration: {file_name} ({old_version} → {new_version})</title>',
        '<style>',
        'body { font-family: Arial, sans-serif; margin: 20px; }',
        '.diff-container { display: flex; }',
        '.diff-pane { flex: 1; margin: 10px; border: 1px solid #ccc; border-radius: 5px; }',
        '.diff-header { background-color: #f0f0f0; padding: 10px; border-bottom: 1px solid #ccc; font-weight: bold; }',
        '.diff-content { padding: 10px; font-family: monospace; white-space: pre; overflow-x: auto; }',
        '.line-number { color: #999; display: inline-block; width: 3em; text-align: right; margin-right: 1em; }',
        '.diff-line { padding: 0 5px; margin: 0; }',
        '.diff-line-added { background-color: #e6ffed; }',
        '.diff-line-removed { background-color: #ffeef0; }',
        '.diff-line-changed { background-color: #fffbdd; }',
        '.summary { margin: 20px 10px; padding: 15px; background-color: #f8f9fa; border: 1px solid #ddd; border-radius: 5px; }',
        '.migration-header { margin-top: 5px; font-weight: bold; }',
        '.migration-item { margin: 5px 0; padding-left: 20px; }',
        '</style>',
        '</head>',
        '<body>',
        f'<h1>Java Migration: {file_name}</h1>',
        f'<h2>Java {old_version} → Java {new_version}</h2>'
    ]
    
    # Add the API migration summary
    migration_summary = []
    migration_patterns = [
        ("Vector", "ArrayList"),
        ("Hashtable", "HashMap"),
        ("Stack", "Deque"),
        ("Enumeration", "Iterator"),
        ("StringTokenizer", "String.split"),
        ("new Date(", "LocalDate"),
        ("SimpleDateFormat", "DateTimeFormatter"),
        ("new FileInputStream", "Files.newInputStream"),
        ("new FileOutputStream", "Files.newOutputStream"),
        ("Thread.stop", "interrupt mechanism"),
        ("addElement", "add"),
        ("elements()", "iterator()")
    ]
    
    for old_api, new_api in migration_patterns:
        if old_api in original_code and old_api not in updated_code:
            if new_api in updated_code and new_api not in original_code:
                migration_summary.append(f"{old_api} → {new_api}")
    
    # Look for import changes
    original_imports = [line for line in original_code.splitlines() if line.strip().startswith("import ")]
    updated_imports = [line for line in updated_code.splitlines() if line.strip().startswith("import ")]
    
    new_imports = set(updated_imports) - set(original_imports)
    if any("java.time" in imp for imp in new_imports):
        migration_summary.append("Added modern java.time package")
    if any("java.nio" in imp for imp in new_imports):
        migration_summary.append("Added modern java.nio file operations")
    
    # Add the summary section to HTML
    html_content.append('<div class="summary">')
    html_content.append('<h3>Migration Summary</h3>')
    
    if migration_summary:
        html_content.append('<div class="migration-header">API Changes:</div>')
        for item in migration_summary:
            html_content.append(f'<div class="migration-item">• {html.escape(item)}</div>')
    else:
        html_content.append('<p>No significant API migrations detected.</p>')
    
    html_content.append('</div>')
    
    # Add the diff container
    html_content.append('<div class="diff-container">')
    
    # Original code pane
    html_content.append('<div class="diff-pane">')
    html_content.append(f'<div class="diff-header">Original (Java {old_version})</div>')
    html_content.append('<div class="diff-content">')
    
    # Compute the diff
    matcher = difflib.SequenceMatcher(None, original_lines, updated_lines)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == 'equal':
            for i in range(i1, i2):
                html_content.append(f'<div class="diff-line"><span class="line-number">{i+1}</span>{html.escape(original_lines[i])}</div>')
        elif tag == 'delete':
            for i in range(i1, i2):
                html_content.append(f'<div class="diff-line diff-line-removed"><span class="line-number">{i+1}</span>{html.escape(original_lines[i])}</div>')
        elif tag == 'replace':
            for i in range(i1, i2):
                html_content.append(f'<div class="diff-line diff-line-changed"><span class="line-number">{i+1}</span>{html.escape(original_lines[i])}</div>')
        elif tag == 'insert':
            for i in range(i1, i2):
                if i < len(original_lines):
                    html_content.append(f'<div class="diff-line"><span class="line-number">{i+1}</span>{html.escape(original_lines[i])}</div>')
    
    html_content.append('</div></div>')
    
    # Updated code pane
    html_content.append('<div class="diff-pane">')
    html_content.append(f'<div class="diff-header">Updated (Java {new_version})</div>')
    html_content.append('<div class="diff-content">')
    
    matcher = difflib.SequenceMatcher(None, original_lines, updated_lines)
    for tag, i1, i2, j1, j2 in matcher.get_opcodes():
        if tag == 'equal':
            for j in range(j1, j2):
                html_content.append(f'<div class="diff-line"><span class="line-number">{j+1}</span>{html.escape(updated_lines[j])}</div>')
        elif tag == 'insert':
            for j in range(j1, j2):
                html_content.append(f'<div class="diff-line diff-line-added"><span class="line-number">{j+1}</span>{html.escape(updated_lines[j])}</div>')
        elif tag == 'replace':
            for j in range(j1, j2):
                html_content.append(f'<div class="diff-line diff-line-changed"><span class="line-number">{j+1}</span>{html.escape(updated_lines[j])}</div>')
        elif tag == 'delete':
            for j in range(j1, j2):
                if j < len(updated_lines):
                    html_content.append(f'<div class="diff-line"><span class="line-number">{j+1}</span>{html.escape(updated_lines[j])}</div>')
    
    html_content.append('</div></div>')
    html_content.append('</div>')  # Close diff-container
    
    # Close HTML
    html_content.append('</body></html>')
    
    return '\n'.join(html_content)

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
    summary_file_path = os.path.join(OUTPUT_DIR, f"{file_name_no_ext}_changes.txt")
    html_report_path = os.path.join(OUTPUT_DIR, f"{file_name_no_ext}_report.html")

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

    # Generate a summary of changes
    print("\n--- Summary of Code Changes ---")
    summary = generate_code_diff_summary(original_java_code, updated_java_code)
    print(summary)

    # 4. Save new .java file and summary
    print(f"\n--- Step 4: Save updated Java code and summary ---")
    try:
        # Save the updated Java code
        with open(updated_java_file_path, "w") as f:
            f.write(updated_java_code)
        print(f"Updated code saved to: {updated_java_file_path}")
        
        # Save the summary to a text file
        with open(summary_file_path, "w") as f:
            f.write(f"Java Code Migration Summary\n")
            f.write(f"==========================\n\n")
            f.write(f"Original file: {input_file_path}\n")
            f.write(f"Updated file: {updated_java_file_path}\n")
            f.write(f"Migration: Java {old_jdk_version} → Java {new_jdk_version}\n\n")
            f.write(summary)
            f.write("\n\n")
            
            # Add the full diff for reference
            f.write("Detailed Diff\n")
            f.write("============\n\n")
            diff = difflib.unified_diff(
                original_java_code.splitlines(),
                updated_java_code.splitlines(),
                fromfile=f"{file_name_no_ext}.java (original)",
                tofile=f"{file_name_no_ext}.java (updated)",
                lineterm=''
            )
            f.write("\n".join(diff))
        
        # Generate and save HTML report with colorized diff
        html_content = generate_html_diff(
            original_java_code, 
            updated_java_code, 
            file_name_no_ext, 
            old_jdk_version, 
            new_jdk_version
        )
        with open(html_report_path, "w") as f:
            f.write(html_content)
            
        print(f"Summary saved to: {summary_file_path}")
        print(f"HTML report with colorized diff saved to: {html_report_path}")
    except Exception as e:
        print(f"Error writing files: {e}")
        return

    print("\nProcess finished.")
    print(f"\nUpdated Java file is saved at: {updated_java_file_path}")
    print(f"Changes summary is saved at: {summary_file_path}")
    print(f"Colorized HTML diff report is saved at: {html_report_path}")

if __name__ == "__main__":
    main() 