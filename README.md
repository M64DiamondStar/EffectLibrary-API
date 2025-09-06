# 📚 EffectLibrary-API

**EffectLibrary-API** is a backend service built in **Kotlin** with [Ktor](https://ktor.io/) and [Exposed](https://github.com/JetBrains/Exposed).  
It provides a REST API for managing **assets**, **tags**, and **API keys**, designed for integration with the Amber Discord bot and other private tools.

⚠️ **Note:** This repository is public for transparency, but the API is intended for **self-hosted use only**.

---

## ✨ Features

- **Asset management**
  - Create, update, delete, and fetch assets
  - Attach tags and metadata
  - Store raw effect data
- **Tag management**
  - Create, rename, delete tags
  - Assign tags to assets
- **Moderation tools**
  - Approve/reject assets
  - Search/filter endpoints
- **API key system**
  - Permission levels
  - Rate limiting
  - `last_used_at` tracking

---

## 🛠️ Tech Stack

- **Kotlin** (JVM 21)
- **Ktor** (HTTP server framework)
- **Exposed** (Kotlin SQL framework)
- **PostgreSQL** (persistent storage)
- **Docker** (deployment)

---

## 📡 API Overview

**Assets**
- GET /assets → Get all approved assets
- GET /assets/get/{id} → Get a single asset
- POST /assets/create → Create a new asset
- PATCH /assets/update/material → Update material for an asset
- PATCH /assets/update/pastelink → Update paste link for an asset
- PATCH /assets/update/tags → Update tags for an asset
- DELETE /assets → Delete an asset
- GET /assets/raw → Get raw effect data

**Tags**
- GET /tags → Get all tags
- POST /tags → Create a tag
- PATCH /tags → Rename a tag
- DELETE /tags → Delete a tag

**API Keys**
- POST /keys/create → Create an API key
- GET /keys/{id} → Get API key by ID

Authentication is handled with Bearer tokens in the Authorization header.
Each route has a minimum permission level enforced via API keys.

---

## 📜 License

This project is licensed under the MIT License.
You are free to read, fork, and learn from this repository, but the hosted API is not for public use.
