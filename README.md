# Ollama Chat Project Builder for Joget

This plugin provides a custom UI page component for Joget that allows users to chat with Ollama, a local LLM (Large Language Model) service. Instead of redirecting users to external services, this component integrates the chat interface directly into Joget's UI and connects to your local Ollama instance.

## Features

- Clean, modern chat interface integrated within Joget
- Support for Ollama API (Llama2, Mistral, etc.)
- Streaming responses for a more interactive experience
- Customizable system prompts
- Adjustable parameters (temperature, max tokens)
- Custom CSS support for UI customization

## Installation

1. Build the plugin using Maven:
   ```
   mvn clean install
   ```

2. Upload the generated JAR file (`target/llm-chat-project-builder-1.0.0.jar`) to Joget:
   - Log in to Joget as an administrator
   - Go to Manage Apps > Manage Plugins
   - Upload the JAR file

## Usage

### Adding to a Userview

1. In the Joget App Designer, open your app and navigate to a userview
2. Add a new menu item and select "Ollama Chat Interface" from the menu type dropdown
3. Configure the following settings:
   - **Menu Label**: The label for the menu item
   - **ID**: A unique identifier for the menu
   - **Ollama API Endpoint**: The API endpoint for your Ollama instance (default: http://localhost:11434/api/generate)
   - **Ollama Model**: The model to use (e.g., "llama2", "mistral", etc.)
   - **Temperature**: Controls randomness (0.0 to 1.0)
   - **Max Tokens**: Maximum number of tokens to generate
   - **System Prompt**: Initial instructions for the LLM
   - **Use Streaming**: Enable to get streaming responses for a more interactive experience
   - **Custom CSS**: Optional CSS to customize the appearance

### Ollama Setup

1. Install Ollama on your server or local machine from [ollama.ai](https://ollama.ai/)
2. Pull your desired models using the Ollama CLI:
   ```
   ollama pull llama2
   ollama pull mistral
   ```
3. Ensure Ollama is running and accessible from your Joget server (default port: 11434)

### Security Considerations

- API keys are stored in the Joget database and should be properly secured
- Consider implementing additional security measures for sensitive applications
- Review your LLM provider's terms of service and data handling policies

## Customization

The plugin is designed to work with Ollama, but can be extended to support additional LLM providers by creating new client classes similar to `OllamaApiClient.java`. The implementation supports both regular and streaming responses for an interactive chat experience.

## License

This project is licensed under the [MIT License](LICENSE).
