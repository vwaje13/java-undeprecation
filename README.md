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
    GOOGLE_APPLICATION_CREDENTIALS=your_application_credentials
    ```

## Setup

1.  **Clone the repository (if applicable) or create the project files.**
2.  **Install Python dependencies**:
    ```bash
    pip install -r requirements.txt
    ```
3.  **Place your input Java file(s)** in the `input/` directory. The script will automatically create this directory if it doesn't exist.

## Usage

Run the script:
```bash
insert script here

## Features

- [Feature 1]
- [Feature 2]
- [Feature 3]

