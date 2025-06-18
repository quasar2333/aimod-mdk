# AI Chat Mod (Minecraft Forge 1.12.2)

## English

AI Chat Mod integrates language model (LLM) services with a Minecraft server. Players can talk to an AI in public chat or start private conversations. The mod sends chat context to an HTTP API compatible with the OpenAI format and prints replies in game. Public and private models are configured via `config/aimod.cfg`.

### Features
- **Public AI chat** – enable or disable a global model that replies in the server chat.
- **Private AI chat** – administrators can start a private session for a player; only that player sees the conversation.
- **Keyword actions** – AI answers can trigger server commands when certain keywords are detected.
- **Persistent sessions** – optional saving of private sessions to disk so players can resume them after relogging.

### How It Works
1. `ChatStateManager` stores chat history and session state for each model or player.
2. When a player sends a message, `ServerChatEventHandler` intercepts it. Depending on the active model it either sends the text to the AI service or lets it through untouched.
3. `AIClient` performs an asynchronous HTTP request. Once the reply arrives, it is delivered back to the Minecraft main thread and shown to the player(s).
4. `PacketHandler` keeps the client in sync, toggling whether normal chat should be hidden during private sessions.

### Commands
```
/aichat public <ModelName> on|off
/aichat private <ModelName> <PlayerSelector> on
/aichat private <PlayerSelector> off
/aichat reload
/aichat list
```
Model profiles are defined in the config file and include API URL, key, system prompt, and other settings.

### Building
Use Java 8 and run `gradlew build`. ForgeGradle will download all dependencies. The output JAR can be placed in the server's `mods` folder.

## 中文

AI Chat Mod 将大型语言模型服务集成到 Minecraft 服务器中。玩家可以在公共聊天中和 AI 对话，或由管理员开启只属于某个玩家的私聊会话。聊天内容通过 HTTP 发送到兼容 OpenAI 接口的服务端，再把 AI 回复显示在游戏里。各个模型的配置存放在 `config/aimod.cfg`。

### 主要功能
- **公共聊天 AI**：开启后，服务器聊天频道会有 AI 回复。
- **私人聊天 AI**：管理员可以让某位玩家与指定模型进行私聊，其他玩家看不到。
- **关键词触发指令**：当 AI 回复中包含设定的关键词时，可自动执行服务器指令。
- **会话持久化**：可选地将私聊会话保存到文件，玩家下次上线时继续对话。

### 实现原理
1. `ChatStateManager` 负责保存各模型或玩家的对话历史与状态。
2. `ServerChatEventHandler` 监听聊天事件，根据当前模式决定是否把内容发送给 AI 服务。
3. `AIClient` 异步执行 HTTP 请求，收到回复后切回主线程，在游戏中显示。
4. 通过 `PacketHandler` 与客户端同步状态，使私聊时其它聊天被屏蔽。

### 指令一览
```
/aichat public <模型名> on|off
/aichat private <模型名> <玩家选择器> on
/aichat private <玩家选择器> off
/aichat reload
/aichat list
```
模型的 API 地址、密钥、系统提示词等均在配置文件中定义。

### 构建方式
请使用 Java 8，运行 `gradlew build`。ForgeGradle 会自动下载依赖，生成的 JAR 放入服务器的 `mods` 文件夹即可。

