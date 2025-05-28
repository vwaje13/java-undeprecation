# Java Deprecation Updater

This project takes a Java source file written for an older JDK (e.g., JDK 17), identifies deprecated API usage,
uses Google's Vertex AI (Gemini) to rewrite the code for a newer JDK (e.g., JDK 21) by replacing deprecated features,
and then compiles and runs both versions to compare their output.

## Prerequisites

1.  **Python 3.7+**: Make sure you have Python installed.
2.  **Java Development Kit (JDK)**:
    *   `jdeprscan`: This tool is part of the JDK (typically found in the `bin` directory). Ensure it's accessible from your terminal. It will be used to identify deprecated APIs based on an older JDK version.
    *   `javac` and `java`: Ensure these are on your PATH and configured for the target newer JDK version (e.g., JDK 21) for compiling and running the code.
3.  **Google Cloud Vertex AI Credentials**: The script uses Google's Vertex AI API for accessing Gemini. You need:
    *   GCP Project ID
    *   Vertex AI API Key
    
    These should be set in the `.env` file:
    ```
    GCP_PROJECT_ID=your_project_id
    VERTEX_API_KEY=your_vertex_api_key
    ```

## Setup

1.  **Clone the repository (if applicable) or create the project files.**
2.  **Install Python dependencies**:
    ```bash
    pip install -r requirements.txt
    ```
3.  **Place your input Java file(s)** in the `input/` directory. The script will automatically create this directory if it doesn't exist.

## Usage

Simply run the main script without any arguments:

```bash
python main.py
```

The script will guide you through an interactive process:

1. It will show you a list of Java files found in the `input/` directory and ask you to select one.
2. It will ask you to select the OLD JDK version (the one your code currently uses).
3. It will ask you to select the NEW JDK version (the one you want to update to).

After making your selections, the script will:
1.  Analyze the input Java file using `jdeprscan` for deprecated elements relative to the selected old JDK version.
2.  Send the code and the list of deprecated elements to Gemini via Vertex AI to get an updated version.
3.  Save the updated code to the `output/` directory.
4.  Compile and run the original Java file.
5.  Compile and run the updated Java file.
6.  Compare their console outputs and report if they match.

All compilation artifacts and output files will be placed in the `output/` directory.

## Features

- [Feature 1]
- [Feature 2]
- [Feature 3]

