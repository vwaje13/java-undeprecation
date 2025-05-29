# Java Modernization Updater

A tool that modernizes Java code for newer JDK versions by identifying deprecated API usage and replacing it with modern equivalents. It uses the Maven Modernizer plugin for analysis and Google's Vertex AI (Gemini) for intelligent code updates.

## Features

- Analyzes Java code using the Modernizer Maven plugin to identify deprecated API usage
- Leverages Google's Vertex AI (Gemini) to intelligently update deprecated code
- Supports modernization to JDK 11, 17, and 21
- Generates a detailed summary of all changes made during modernization

## Prerequisites

1. **Python 3.7+**: Required to run the script
2. **Maven**: Required for the Modernizer plugin
3. **Google Cloud Vertex AI Access**: 
   - GCP Project ID
   - Service Account Credentials

## Setup

1. **Clone this repository**
   ```bash
   git clone https://github.com/yourusername/java-undeprecation.git
   cd java-undeprecation
   ```

2. **Install Python dependencies**
   ```bash
   pip install python-dotenv vertexai
   ```

3. **Configure Google Cloud credentials**
   
   Create a `.env` file in the project root with the following:
   ```
   GCP_PROJECT_ID=your_project_id
   GOOGLE_APPLICATION_CREDENTIALS=path/to/your/credentials.json
   ```

4. **Place Java files for modernization**
   
   Put the Java files you want to modernize in the `input/` directory.

## Usage

1. **Run the script**
   ```bash
   python main.py
   ```

2. **Follow the interactive prompts**
   - Select the Java file to modernize
   - Choose the target JDK version (11, 17, or 21)

3. **Review the results**
   
   After processing, the script will create:
   - The modernized Java file in the `output/` directory
   - A detailed report of all changes in `output/[filename]_modernizer_changes.txt`

## How It Works

1. The script analyzes your Java code using the Modernizer Maven plugin
2. It identifies deprecated APIs and other outdated patterns
3. The findings are sent to Google's Gemini model along with your code
4. Gemini intelligently updates the code while preserving functionality
5. The updated code and a detailed change report are saved to the output directory

## Example

Input Java file with deprecated code:
```java
import java.util.Date;
public class Example {
    public void legacyMethod() {
        Date date = new Date();
        date.getHours(); // Deprecated method
    }
}
```

After modernization to JDK 17:
```java
import java.time.LocalDateTime;
public class Example {
    public void legacyMethod() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour(); // Modern equivalent
    }
}
```

## Limitations

- Requires Maven to be installed and accessible
- Requires Google Cloud credentials with Vertex AI access
- Only analyzes and updates Java source files
- The quality of modernization depends on the Gemini model's understanding of the code

