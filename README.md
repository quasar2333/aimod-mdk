# AI Chat Mod (Minecraft Forge 1.12.2)

<!-- This README will be bilingual. English sections will appear first, followed by Chinese sections. -->

## Table of Contents (English)
- [Overview](#overview)
- [Features](#features)
- [How It Works (Conceptual)](#how-it-works-conceptual)
- [Core Components](#core-components)
  - [Package Structure](#package-structure)
  - [Key Classes and Their Roles](#key-classes-and-their-roles)
- [Configuration (`config/aimod.cfg`)](#configuration-configaimodcfg)
  - [General Settings (`[general]`)](#general-settings-general)
  - [Model Profile Settings (`[model_<ProfileName>]`)](#model-profile-settings-model_profilename)
  - [Default Model Profiles](#default-model-profiles)
- [Commands (`/aichat`)](#commands-aichat)
- [API Interaction](#api-interaction)
- [Session Persistence](#session-persistence)
- [Building from Source](#building-from-source)
- [For Developers](#for-developers)
- [Credits](#credits)
- [License](#license)

## 目录 (中文)
- [概述](#概述)
- [主要功能](#主要功能)
- [实现原理（概念）](#实现原理概念)
- [核心组件](#核心组件)
  - [包结构](#包结构)
  - [关键类及其职责](#关键类及其职责)
- [配置文件 (`config/aimod.cfg`)](#配置文件-configaimodcfg)
  - [全局设置 (`[general]`)](#全局设置-general)
  - [模型配置 (`[model_<ProfileName>]`)](#模型配置-model_profilename)
  - [默认模型配置](#默认模型配置)
- [指令 (`/aichat`)](#指令-aichat)
- [API 交互](#api-交互)
- [会话持久化](#会话持久化)
- [从源码构建](#从源码构建)
- [给开发者](#给开发者)
- [致谢](#致谢)
- [许可证](#许可证)

---

## English

### Overview

AI Chat Mod integrates Large Language Model (LLM) services with a Minecraft Forge 1.12.2 server. It allows players to interact with AI through server chat. Players can converse with a global AI in public chat, or administrators can initiate private AI conversations for individual players. The mod sends chat context (history and current message) to an HTTP API (compatible with the OpenAI completions API format) and displays the AI's replies in the game. Multiple AI models, their API details, and behavior can be configured in the `config/aimod.cfg` file.

### Features

-   **Public AI Chat**: Enable or disable a global AI model that participates in the server's public chat.
-   **Private AI Chat**: Server administrators can start private AI sessions for specific players. In a private session, the player's messages are sent only to the AI, and other players' public chat messages are hidden from the participating player's view.
-   **Configurable Models**: Define multiple AI model profiles in the configuration file, each with its own API endpoint, key, system prompt, and other parameters.
-   **Keyword Actions**: Configure server commands to be automatically executed when the AI's response contains specific keywords. For example, if the AI says "Here is a diamond for you!", a command can give the player a diamond.
-   **Persistent Private Sessions**: Optionally, private chat sessions can be saved to disk. This allows players to resume their private AI conversations even after logging out and back in.
-   **OpenAI API Compatibility**: Designed to work with any LLM backend that provides an OpenAI-compatible chat completions API endpoint.

### How It Works (Conceptual)

1.  **Player Input**: A player sends a message in the Minecraft chat.
2.  **Event Interception**: The `ServerChatEventHandler` intercepts the player's message.
3.  **Session Management**:
    *   If the player is in a private AI session, their message is captured, and the original chat event is canceled (it doesn't appear in public chat).
    *   If a public AI model is active and the player is not in a private session, their message is processed for the public AI.
4.  **Context Assembly**: The `ChatStateManager` retrieves the relevant chat history for the current session (public or private) and combines it with the new message and the configured system prompt for the AI model.
5.  **API Communication**: The `AIClient` asynchronously sends this compiled context in a JSON request to the configured HTTP API endpoint for the selected AI model.
6.  **Response Handling**: Once the LLM API sends back a response, `AIClient` parses it.
7.  **Display in Game**: The AI's reply is then formatted and displayed either in public chat or privately to the player, depending on the session type.
8.  **Keyword Check**: The AI's response is checked for any configured keywords. If a match is found, the corresponding server command(s) are executed.
9.  **Client Synchronization (Private Chat)**: For private sessions, the `PacketHandler` and a custom network message (`MessageTogglePrivate`) are used to inform the player's client to hide public chat messages, creating an immersive private conversation experience.

### Core Components

#### Package Structure

The mod's source code is organized into the following main packages under `com.yourname.aimod`:

-   `ai/`: Contains the core AI interaction logic.
    -   `api/`: Data classes (POJOs) for structuring requests to and responses from the OpenAI-compatible API (e.g., `ApiRequest`, `ApiResponse`, `ApiMessage`).
-   `command/`: Houses the implementation for the `/aichat` command.
-   `event/`: Includes Minecraft event listeners, primarily for chat events and player login/logout.
-   `network/`: Manages custom network packets for server-client communication (e.g., toggling private chat mode).
-   `proxy/`: Standard Forge proxies for client-side and server-side specific initializations.
-   `util/`: Utility classes, for example, for chat message formatting.

#### Key Classes and Their Roles

-   **`AiMod.java`**: The main mod class. It handles Forge lifecycle events (pre-initialization, initialization, server starting/stopping) and registers necessary components like commands and event handlers.
-   **`ModConfig.java`**: Responsible for loading, parsing, and managing the mod's configuration from the `config/aimod.cfg` file. It defines the structure of the configuration and provides access to the settings.
-   **`ModelProfile.java`**: A data class (POJO) that holds the configuration settings for a single AI model (e.g., API URL, key, system prompt, temperature).
-   **`ai.AIClient.java`**: This class handles all asynchronous HTTP communication with the external LLM API. It constructs the JSON request, sends it, receives the response, and parses it.
-   **`ai.ChatStateManager.java`**: A crucial singleton class that manages the state of all AI interactions. It keeps track of:
    *   The currently active public AI model.
    *   Active private AI sessions for players (mapping player UUIDs to model names).
    *   Chat histories for both public and private sessions, using evicting queues to maintain context length.
    *   Handles the logic for saving and loading private sessions if persistence is enabled.
    *   Manages a "processing" flag to prevent concurrent API calls for the same session.
-   **`command.CommandAiChat.java`**: Implements all functionality for the `/aichat` command, including its subcommands for managing public and private AI, reloading configuration, and listing models.
-   **`event.ServerChatEventHandler.java`**: Listens for server-side chat events (`ServerChatEvent`). It determines if a message should be processed by an AI (public or private), interacts with `ChatStateManager` and `AIClient` to get an AI response, and handles player login/logout events for session management (restoring or clearing sessions).
-   **`event.ClientChatEventHandler.java`**: Listens for client-side chat events (`ClientChatReceivedEvent`). Its primary role, when a player is in a private AI session, is to suppress incoming public chat messages from other players, based on signals from the server via `MessageTogglePrivate`.
-   **`network.PacketHandler.java`**: Standard Forge system for registering and sending/receiving custom network messages between the server and clients.
-   **`network.MessageTogglePrivate.java`**: A custom packet sent from the server to a client. It tells the client whether to enable or disable the "private mode" UI (i.e., whether to hide public chat messages).

### Configuration (`config/aimod.cfg`)

The mod's behavior is primarily controlled by the `aimod.cfg` file, located in your Minecraft server's `config` directory. If the file doesn't exist, it will be generated with default values when the server starts with the mod.

The configuration is split into a general section and multiple model profile sections.

#### General Settings (`[general]`)

This section contains global settings for the mod.

-   `persistPrivateSessions` (Boolean, Default: `false`)
    *   If `true`, private AI chat sessions (the model a player is talking to and their chat history) will be saved when a player logs out or the server stops. The session will be restored when the player logs back in.
    *   If `false`, private AI sessions end when the player logs out. Chat history for these sessions is not saved.

#### Model Profile Settings (`[model_<ProfileName>]`)

You can define multiple AI model profiles. Each profile is defined in its own configuration category, named with the prefix `model_`. For example, a model profile named "helper" would be configured under the `[model_helper]` category.

For each model profile, the following options are available:

-   `isPublic` (Boolean, Default: `false` for new profiles, varies for defaults)
    *   Set to `true` if this model can be used as the global public AI in server chat.
    *   Set to `false` if this model is intended only for private AI sessions initiated by an admin.
-   `baseUrl` (String, Default: `http://localhost:1234/v1/chat/completions`)
    *   The full HTTP(S) URL of the OpenAI-compatible chat completions API endpoint.
-   `apiKey` (String, Default: `lm-studio` or `no-key`)
    *   The API key for the LLM service. If the service does not require an API key, you can set this to `none`, `no-key`, or leave it empty. The mod will send it as a Bearer token in the `Authorization` header if a value is provided.
-   `modelId` (String, Default: varies, e.g., `model-id-from-provider`)
    *   The specific model identifier string that the API provider expects (e.g., `gpt-3.5-turbo`, `llava-7b-v1.5`, etc.).
-   `temperature` (Float, Range: `0.0` - `2.0`, Default: `0.7`)
    *   Controls the randomness and creativity of the AI's responses. Higher values (e.g., 1.0-1.5) make the output more random and creative, while lower values (e.g., 0.2-0.5) make it more focused and deterministic.
-   `maxContext` (Integer, Range: `1` - `100`, Default: `5`)
    *   The maximum number of past messages (pairs of user messages and AI responses) to keep in the chat history sent to the AI. This helps the AI remember the context of the conversation. A larger number means more context but also a larger API request.
-   `systemPrompt` (String, Default: "You are a helpful assistant in Minecraft.")
    *   A message sent to the AI at the beginning of a conversation to set its persona, role, or provide specific instructions on how it should behave or respond.
-   `keywordActions` (String List, Default: empty or example actions)
    *   Defines automated server commands to be executed when the AI's response contains certain keywords.
    *   **Format**: Each entry in the list must be a string in the format: `"keyword::/command1 with @p|/command2 arg"`.
        *   `keyword`: The specific word or phrase (case-insensitive) to look for in the AI's response.
        *   `::`: A separator between the keyword and the commands.
        *   `/command1|/command2`: One or more server commands, separated by `|`. These commands are executed by the server console.
        *   `@p`: You can use `@p` within your commands as a placeholder for the name of the player who triggered the AI response (either by sending the message in public chat or being the subject of a private chat). The mod will replace `@p` with the player's actual name before executing the command.
    *   **Example**: `"open the door::/say Door opened for @p|/setblock 123 64 456 minecraft:air"`

#### Default Model Profiles

If `aimod.cfg` is missing or new, the mod will create two default model profiles to get you started:

-   `[model_public_bot]`: A sample public model.
-   `[model_private_guide]`: A sample private model.

These defaults use a local API endpoint (`http://127.0.0.1:1234/v1/chat/completions`) common for tools like LM Studio. You will need to adjust these settings to match your actual LLM service provider.

### Commands (`/aichat`)

The mod provides the `/aichat` command for managing AI interactions. All subcommands require operator (OP) privileges (permission level 2).

-   `/aichat public <ModelName> on`
    *   Enables the global public AI chat using the specified `<ModelName>`. The model must be configured with `isPublic=true`.
    *   Example: `/aichat public public_bot on`
-   `/aichat public off`
    *   Disables the global public AI chat.
-   `/aichat private <ModelName> <PlayerSelector> on`
    *   Starts a private AI chat session for the specified player(s) (`<PlayerSelector>`, e.g., a player name, `@a`, `@p`) using the AI model `<ModelName>`. The model must be configured with `isPublic=false`.
    *   Example: `/aichat private private_guide Notch on`
-   `/aichat private <PlayerSelector> off`
    *   Ends the private AI chat session for the specified player(s).
    *   Example: `/aichat private Notch off`
-   `/aichat reload`
    *   Reloads the `aimod.cfg` configuration file from disk. This will also clear all current AI states, including active sessions and chat histories.
-   `/aichat list`
    *   Lists all available AI model profiles defined in the configuration file, indicating whether they are `PUBLIC` or `PRIVATE`.
    *   Also shows which public model, if any, is currently active.

### API Interaction

The mod is designed to communicate with LLM services that offer an OpenAI-compatible chat completions API.

-   **Compatibility**: It expects an HTTP endpoint that accepts JSON requests and returns JSON responses similar to OpenAI's `/v1/chat/completions` API.
-   **Request Format**: For each AI query, the mod sends a POST request with a JSON body containing:
    *   `model`: The `modelId` from the model profile.
    *   `messages`: An array of message objects. This includes:
        *   The `systemPrompt` (as a message with role `system`).
        *   A sequence of past user messages (role `user`) and assistant responses (role `assistant`) from the current session's history, up to `maxContext`.
        *   The latest user message (role `user`).
    *   `temperature`: The `temperature` value from the model profile.
-   **Response Format**: The mod expects a JSON response containing an array called `choices`. It reads the AI's message from `choices[0].message.content`.
-   **Authentication**: If an `apiKey` is provided in the model profile, it is sent in the `Authorization` header as `Bearer <apiKey>`.

### Session Persistence

Private AI chat sessions can be made persistent across server restarts or player relogs.

-   **Activation**: This feature is enabled by setting `persistPrivateSessions=true` in the `[general]` section of `config/aimod.cfg`.
-   **Functionality**:
    *   When enabled, if a player is in a private AI session and logs out, their current session (which model they were talking to and the recent chat history with that model) is saved.
    *   When the player logs back in, their private AI session is automatically restored.
    *   If the server is stopped while sessions are active and persistence is enabled, these sessions are saved. They will be available when the server restarts.
-   **Storage Location**: Session data is stored in a JSON file named `aimod_sessions.json` located inside the `data` folder of your Minecraft world's save directory (e.g., `<your_server_root>/world/data/aimod_sessions.json`).

### Building from Source

To build the AI Chat Mod from its source code:

1.  **Prerequisites**:
    *   Java Development Kit (JDK) 8.
2.  **Clone the Repository**: Get a copy of the source code, typically using Git.
3.  **Build**:
    *   Open a terminal or command prompt in the root directory of the project.
    *   Run the Gradle wrapper command:
        *   On Linux/macOS: `./gradlew build`
        *   On Windows: `gradlew.bat build`
4.  **Output**:
    *   The compiled mod JAR file will be located in the `build/libs/` directory (e.g., `build/libs/MyMod-1.0.0.jar`).
5.  **Installation**:
    *   Place the generated JAR file into your Minecraft Forge server's `mods` folder.

### For Developers

If you wish to contribute to the mod or set up a development environment:

-   **Setup**: Follow the official Minecraft Forge documentation for setting up your preferred IDE (Eclipse or IntelliJ IDEA). You can find these guides at: [Minecraft Forge Documentation - Getting Started](http://mcforge.readthedocs.io/en/latest/gettingstarted/)
-   **Key Gradle Tasks**:
    *   `gradlew genEclipseRuns` or `gradlew eclipse`: To generate Eclipse project files and run configurations.
    *   `gradlew genIntellijRuns`: To generate IntelliJ IDEA run configurations.
    *   `gradlew --refresh-dependencies`: To force Gradle to re-download dependencies.
    *   `gradlew clean`: To remove build artifacts.

### Credits

-   **Author**: 究极凯文 (as per `mcmod.info`)
-   **Inspiration & Core Concepts**: This mod builds upon common patterns for integrating AI into game environments.
-   **Libraries**: Uses Google's Gson library (typically bundled with Minecraft/Forge) for JSON processing.

### License

Please refer to the `LICENSE.txt` file included with this mod for licensing information.
(The provided files also include `LICENSE-Paulscode IBXM Library.txt` and `LICENSE-Paulscode SoundSystem CodecIBXM.txt`, which are standard in Forge environments, and a main `LICENSE.txt` which would contain the mod's specific license).

---

## 中文 (Chinese)

### 概述

AI Chat Mod (AI聊天模块) 将大型语言模型 (LLM) 服务集成到 Minecraft Forge 1.12.2 服务器中。它允许玩家通过服务器聊天与 AI 进行互动。玩家可以在公共聊天中与全局 AI 对话，或者由管理员为个别玩家发起私人 AI 对话。该模块会将聊天上下文（历史记录和当前消息）发送到兼容 OpenAI Completions API 格式的 HTTP API，并在游戏中显示 AI 的回复。多个 AI 模型、它们的 API 详细信息以及行为都可以在 `config/aimod.cfg` 文件中进行配置。

### 主要功能

-   **公共聊天 AI**：启用或禁用一个参与服务器公共聊天的全局 AI 模型。
-   **私人聊天 AI**：服务器管理员可以为特定玩家开启私人 AI 会话。在私人会话中，玩家的消息仅发送给 AI，其他玩家的公共聊天消息将对参与会话的玩家隐藏。
-   **可配置模型**：在配置文件中定义多个 AI 模型配置，每个配置都有其自己的 API 地址、密钥、系统提示词和其他参数。
-   **关键词触发指令**：可配置当 AI 的回复包含特定关键词时自动执行服务器指令。例如，如果 AI 说“这是给你的钻石！”，一条指令可以给予玩家一颗钻石。
-   **私人会话持久化**：可选择将私人聊天会话保存到磁盘。这允许玩家在注销并重新登录后继续他们的私人 AI 对话。
-   **OpenAI API 兼容性**：设计用于与任何提供 OpenAI 兼容的聊天补全 (Chat Completions) API 端点的 LLM 后端服务一同工作。

### 实现原理（概念）

1.  **玩家输入**：玩家在 Minecraft 聊天中发送一条消息。
2.  **事件拦截**：`ServerChatEventHandler` 拦截玩家的消息。
3.  **会话管理**：
    *   如果玩家处于私人 AI 会话中，他们的消息将被捕获，原始聊天事件将被取消（不会出现在公共聊天中）。
    *   如果公共 AI 模型已激活且玩家未处于私人会话中，他们的消息将为公共 AI 处理。
4.  **上下文组装**：`ChatStateManager` 检索当前会话（公共或私人）的相关聊天记录，并将其与新消息以及为 AI 模型配置的系统提示词组合。
5.  **API 通信**：`AIClient` 以异步方式将此编译后的上下文通过 JSON 请求发送到为所选 AI 模型配置的 HTTP API 端点。
6.  **响应处理**：一旦 LLM API 返回响应，`AIClient` 会解析它。
7.  **游戏中显示**：然后，AI 的回复会根据会话类型被格式化并在公共聊天中显示或私下显示给玩家。
8.  **关键词检查**：检查 AI 的回复是否包含任何已配置的关键词。如果找到匹配项，则执行相应的服务器指令。
9.  **客户端同步（私聊）**：对于私人会话，使用 `PacketHandler` 和自定义网络消息 (`MessageTogglePrivate`) 通知玩家的客户端隐藏公共聊天消息，从而创造沉浸式的私人对话体验。

### 核心组件

#### 包结构

该模块的源代码组织在 `com.yourname.aimod` 下的以下主要包中：

-   `ai/`: 包含核心 AI 交互逻辑。
    -   `api/`: 用于构造发送到 OpenAI 兼容 API 的请求和接收其响应的数据类 (POJO) (例如 `ApiRequest`, `ApiResponse`, `ApiMessage`)。
-   `command/`: 存放 `/aichat` 指令的实现。
-   `event/`: 包括 Minecraft 事件监听器，主要用于聊天事件和玩家登录/登出。
-   `network/`: 管理用于服务器-客户端通信的自定义网络数据包 (例如，切换私聊模式)。
-   `proxy/`: 标准的 Forge 代理，用于客户端和服务器端的特定初始化。
-   `util/`: 工具类，例如用于聊天消息格式化。

#### 关键类及其职责

-   **`AiMod.java`**: 主模块类。处理 Forge 生命周期事件（预初始化、初始化、服务器启动/停止）并注册必要的组件，如指令和事件处理器。
-   **`ModConfig.java`**: 负责从 `config/aimod.cfg` 文件加载、解析和管理模块的配置。它定义了配置的结构并提供对设置的访问。
-   **`ModelProfile.java`**: 一个数据类 (POJO)，用于保存单个 AI 模型的配置设置 (例如 API URL、密钥、系统提示词、temperature)。
-   **`ai.AIClient.java`**: 此类处理与外部 LLM API 的所有异步 HTTP 通信。它构造 JSON 请求，发送请求，接收响应并解析它。
-   **`ai.ChatStateManager.java`**: 一个关键的单例类，管理所有 AI 交互的状态。它跟踪：
    *   当前活动的公共 AI 模型。
    *   玩家的活动私人 AI 会话（将玩家 UUID 映射到模型名称）。
    *   公共和私人会话的聊天记录，使用具有固定容量的队列 (Evicting Queues) 来维护上下文长度。
    *   处理在启用持久化时保存和加载私人会话的逻辑。
    *   管理一个“处理中”标志，以防止对同一会话的并发 API 调用。
-   **`command.CommandAiChat.java`**: 实现 `/aichat` 指令的所有功能，包括其用于管理公共和私人 AI、重新加载配置和列出模型的子指令。
-   **`event.ServerChatEventHandler.java`**: 监听服务器端聊天事件 (`ServerChatEvent`)。它确定消息是否应由 AI 处理（公共或私人），与 `ChatStateManager` 和 `AIClient` 交互以获取 AI 响应，并处理玩家登录/登出事件以进行会话管理（恢复或清除会话）。
-   **`event.ClientChatEventHandler.java`**: 监听客户端聊天事件 (`ClientChatReceivedEvent`)。当玩家处于私人 AI 会话中时，其主要作用是根据服务器通过 `MessageTogglePrivate` 发送的信号，抑制来自其他玩家的传入公共聊天消息。
-   **`network.PacketHandler.java`**: 标准的 Forge 系统，用于在服务器和客户端之间注册和发送/接收自定义网络消息。
-   **`network.MessageTogglePrivate.java`**: 从服务器发送到客户端的自定义数据包。它告诉客户端是启用还是禁用“私聊模式”界面（即是否隐藏公共聊天消息）。

### 配置文件 (`config/aimod.cfg`)

模块的行为主要由位于 Minecraft 服务器 `config` 目录中的 `aimod.cfg` 文件控制。如果该文件不存在，当服务器与模块一同启动时，它将以默认值生成。

配置分为一个通用部分和多个模型配置部分。

#### 全局设置 (`[general]`)

此部分包含模块的全局设置。

-   `persistPrivateSessions` (布尔型, 默认值: `false`)
    *   如果为 `true`，私人 AI 聊天会话（玩家正在对话的模型及其聊天记录）将在玩家注销或服务器停止时保存。当玩家重新登录时，会话将被恢复。
    *   如果为 `false`，私人 AI 会话在玩家注销时结束。这些会话的聊天记录不会被保存。

#### 模型配置 (`[model_<ProfileName>]`)

您可以定义多个 AI 模型配置。每个配置都在其自己的配置类别中定义，名称以 `model_` 为前缀。例如，名为 "helper" 的模型配置将在 `[model_helper]` 类别下配置。

对于每个模型配置，可以使用以下选项：

-   `isPublic` (布尔型, 新配置默认为 `false`, 默认生成的配置值不同)
    *   如果此模型可用作服务器聊天中的全局公共 AI，则设置为 `true`。
    *   如果此模型仅用于由管理员发起的私人 AI 会话，则设置为 `false`。
-   `baseUrl` (字符串, 默认值: `http://localhost:1234/v1/chat/completions`)
    *   OpenAI 兼容的聊天补全 API 端点的完整 HTTP(S) URL。
-   `apiKey` (字符串, 默认值: `lm-studio` 或 `no-key`)
    *   LLM 服务的 API 密钥。如果服务不需要 API 密钥，您可以将其设置为 `none`、`no-key` 或留空。如果提供了值，模块会将其作为 Bearer 令牌在 `Authorization` 请求头中发送。
-   `modelId` (字符串, 默认值: 不定, 例如 `model-id-from-provider`)
    *   API 提供商期望的特定模型标识符字符串 (例如 `gpt-3.5-turbo`, `llava-7b-v1.5` 等)。
-   `temperature` (浮点型, 范围: `0.0` - `2.0`, 默认值: `0.7`)
    *   控制 AI 回复的随机性和创造性。较高的值 (例如 1.0-1.5) 使输出更随机和富有创造性，而较低的值 (例如 0.2-0.5) 使其更专注和确定。
-   `maxContext` (整型, 范围: `1` - `100`, 默认值: `5`)
    *   在发送给 AI 的聊天记录中保留的过去消息（用户消息和 AI 回复的配对）的最大数量。这有助于 AI 记住对话的上下文。数量越大意味着上下文越多，但 API 请求也越大。
-   `systemPrompt` (字符串, 默认值: "You are a helpful assistant in Minecraft.")
    *   在对话开始时发送给 AI 的消息，用于设置其角色、身份或提供关于其应如何行为或回应的具体说明。
-   `keywordActions` (字符串列表, 默认值: 空列表或示例操作)
    *   定义当 AI 的回复包含某些关键词时要自动执行的服务器指令。
    *   **格式**: 列表中的每个条目都必须是以下格式的字符串：`"keyword::/command1 with @p|/command2 arg"`。
        *   `keyword`: 要在 AI 回复中查找的特定词或短语（不区分大小写）。
        *   `::`: 关键词和指令之间的分隔符。
        *   `/command1|/command2`: 一个或多个服务器指令，用 `|` 分隔。这些指令由服务器控制台执行。
        *   `@p`: 您可以在指令中使用 `@p` 作为触发 AI 回复的玩家名称的占位符（无论是在公共聊天中发送消息还是作为私聊的对象）。模块在执行指令前会将 `@p` 替换为玩家的实际名称。
    *   **示例**: `"打开门::/say 已为@p开门|/setblock 123 64 456 minecraft:air"`

#### 默认模型配置

如果 `aimod.cfg` 缺失或为新创建，模块将创建两个默认模型配置以帮助您开始：

-   `[model_public_bot]`: 一个示例公共模型。
-   `[model_private_guide]`: 一个示例私人模型。

这些默认配置使用本地 API 端点 (`http://127.0.0.1:1234/v1/chat/completions`)，这对于像 LM Studio 这样的工具很常见。您需要调整这些设置以匹配您实际的 LLM 服务提供商。

### 指令 (`/aichat`)

该模块提供 `/aichat` 指令来管理 AI 交互。所有子指令都需要操作员 (OP) 权限（权限等级 2）。

-   `/aichat public <ModelName> on`
    *   使用指定的 `<ModelName>` 启用全局公共 AI 聊天。该模型必须配置为 `isPublic=true`。
    *   示例: `/aichat public public_bot on`
-   `/aichat public off`
    *   禁用全局公共 AI 聊天。
-   `/aichat private <ModelName> <PlayerSelector> on`
    *   为指定的玩家 (`<PlayerSelector>`，例如玩家名称, `@a`, `@p`) 使用 AI 模型 `<ModelName>` 开启私人 AI 聊天会话。该模型必须配置为 `isPublic=false`。
    *   示例: `/aichat private private_guide Notch on`
-   `/aichat private <PlayerSelector> off`
    *   结束指定玩家的私人 AI 聊天会话。
    *   示例: `/aichat private Notch off`
-   `/aichat reload`
    *   从磁盘重新加载 `aimod.cfg` 配置文件。这也将清除所有当前的 AI 状态，包括活动会话和聊天记录。
-   `/aichat list`
    *   列出配置文件中定义的所有可用 AI 模型配置，并指明它们是 `PUBLIC` (公共) 还是 `PRIVATE` (私人)。
    *   同时显示当前活动的公共模型（如果有）。

### API 交互

该模块设计用于与提供 OpenAI 兼容的聊天补全 API 的 LLM 服务进行通信。

-   **兼容性**: 它期望一个 HTTP 端点，该端点接受 JSON 请求并返回类似于 OpenAI 的 `/v1/chat/completions` API 的 JSON 响应。
-   **请求格式**: 对于每个 AI 查询，模块发送一个带有 JSON 主体的 POST 请求，其中包含：
    *   `model`: 来自模型配置的 `modelId`。
    *   `messages`: 一个消息对象数组。这包括：
        *   `systemPrompt` (作为角色为 `system` 的消息)。
        *   来自当前会话历史记录的一系列过去的用户消息 (角色 `user`) 和助手回复 (角色 `assistant`)，最多为 `maxContext` 条。
        *   最新的用户消息 (角色 `user`)。
    *   `temperature`: 来自模型配置的 `temperature` 值。
-   **响应格式**: 模块期望一个包含名为 `choices` 的数组的 JSON 响应。它从 `choices[0].message.content` 中读取 AI 的消息。
-   **身份验证**: 如果在模型配置中提供了 `apiKey`，它将作为 `Bearer <apiKey>` 在 `Authorization` 请求头中发送。

### 会话持久化

私人 AI 聊天会话可以在服务器重启或玩家重新登录后保持持久。

-   **激活**: 通过在 `config/aimod.cfg` 的 `[general]` 部分设置 `persistPrivateSessions=true` 来启用此功能。
-   **功能**:
    *   启用后，如果玩家处于私人 AI 会话中并注销，他们当前的会话（他们正在与之交谈的模型以及与该模型的近期聊天记录）将被保存。
    *   当玩家重新登录时，他们的私人 AI 会话将自动恢复。
    *   如果在会话活动且启用了持久化的情况下停止服务器，这些会话将被保存。它们将在服务器重启时可用。
-   **存储位置**: 会话数据存储在名为 `aimod_sessions.json` 的 JSON 文件中，该文件位于您的 Minecraft 世界存档目录的 `data` 文件夹内 (例如 `<您的服务器根目录>/world/data/aimod_sessions.json`)。

### 从源码构建

要从其源代码构建 AI Chat Mod：

1.  **先决条件**:
    *   Java Development Kit (JDK) 8。
2.  **克隆仓库**: 获取源代码的副本，通常使用 Git。
3.  **构建**:
    *   在项目的根目录中打开终端或命令提示符。
    *   运行 Gradle包装器 (wrapper) 命令：
        *   在 Linux/macOS 上: `./gradlew build`
        *   在 Windows 上: `gradlew.bat build`
4.  **输出**:
    *   编译后的模块 JAR 文件将位于 `build/libs/` 目录中 (例如 `build/libs/MyMod-1.0.0.jar`)。
5.  **安装**:
    *   将生成的 JAR 文件放入您的 Minecraft Forge 服务器的 `mods` 文件夹中。

### 给开发者

如果您希望为该模块做出贡献或设置开发环境：

-   **设置**: 请遵循官方的 Minecraft Forge 文档来设置您偏好的 IDE (Eclipse 或 IntelliJ IDEA)。您可以在以下位置找到这些指南：[Minecraft Forge Documentation - Getting Started](http://mcforge.readthedocs.io/en/latest/gettingstarted/)
-   **关键 Gradle 任务**:
    *   `gradlew genEclipseRuns` 或 `gradlew eclipse`: 生成 Eclipse 项目文件和运行配置。
    *   `gradlew genIntellijRuns`: 生成 IntelliJ IDEA 运行配置。
    *   `gradlew --refresh-dependencies`: 强制 Gradle 重新下载依赖项。
    *   `gradlew clean`: 清除构建产物。

### 致谢

-   **作者**: 究极凯文 (根据 `mcmod.info`)
-   **灵感与核心概念**: 此模块基于将 AI 集成到游戏环境中的常见模式构建。
-   **库**: 使用 Google 的 Gson 库（通常与 Minecraft/Forge 捆绑）进行 JSON 处理。

### 许可证

有关许可信息，请参阅此模块附带的 `LICENSE.txt` 文件。
(所提供的文件还包括 `LICENSE-Paulscode IBXM Library.txt` 和 `LICENSE-Paulscode SoundSystem CodecIBXM.txt`，这些是 Forge 环境中的标准文件，以及一个主要的 `LICENSE.txt`，其中将包含模块特定的许可证)。
