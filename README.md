# LLM Chat Project Builder for Joget

This plugin provides a custom UI page component for Joget that allows users to chat with an LLM (Large Language Model) via API. Instead of redirecting users to the LLM's service page, this component integrates the chat interface directly into Joget's UI.

## Features

- Clean, modern chat interface integrated within Joget
- Support for OpenAI API (GPT-3.5, GPT-4, etc.)
- Extensible design to support other LLM providers
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
2. Add a new menu item and select "LLM Chat Interface" from the menu type dropdown
3. Configure the following settings:
   - **Menu Label**: The label for the menu item
   - **ID**: A unique identifier for the menu
   - **LLM API Endpoint**: The API endpoint for your LLM provider (default: OpenAI)
   - **API Key**: Your API key for the LLM service
   - **LLM Model**: The model to use (e.g., "gpt-4")
   - **Temperature**: Controls randomness (0.0 to 1.0)
   - **Max Tokens**: Maximum number of tokens to generate
   - **System Prompt**: Initial instructions for the LLM
   - **Custom CSS**: Optional CSS to customize the appearance

### Security Considerations

- API keys are stored in the Joget database and should be properly secured
- Consider implementing additional security measures for sensitive applications
- Review your LLM provider's terms of service and data handling policies

## Customization

The plugin can be extended to support additional LLM providers by modifying the `LlmApiClient.java` file. The default implementation supports OpenAI's API format, but you can add support for other providers by implementing custom API call methods.

## License

This project is licensed under the [MIT License](LICENSE).
