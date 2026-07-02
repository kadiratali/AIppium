# 🚀 Appium QA Automation Framework

> **A modern, scalable, and reusable Appium-based QA automation framework for mobile testing.**

## 🧩 Overview

This framework has been developed to accelerate test processes, ensure reusability, and detect issues early in mobile applications.  
It supports both **Android** and **iOS** platforms and provides an easy way to manage and execute automated mobile tests.

### ✨ Key Features
- 🌐 Cross-platform support (Android & iOS)
- 🧠 Modular structure using **Page Object Model (POM)**
- ⚙️ Parallel test execution support
- 📸 Automatic screenshot capture on failure
- 🧾 Detailed reporting with **Allure**
- 🧪 Optional **Cucumber BDD** integration

---

## 🏗️ Tech Stack

| Technology     | Description |
|----------------|-------------|
| **Java 17**    | Core programming language |
| **Appium 3.x** | Mobile automation framework |
| **TestNG**     | Test execution framework |
| **Maven**      | Build and dependency management |
| **Allure**     | Test reporting |
| **SLF4J**      | Logging utility |

---

## 📁 Project Structure


## 🏃 Running Tests
To execute the test cases in this framework, ensure that you have completed the necessary configuration as described in the Configuration section and that the Appium server is running. Follow these steps to run the tests:

**Start the Appium Server:** Ensure that the Appium server is running and accessible. The URL for the Appium server should match the `appiumServerUrl` value set in `src/config.json`

### Runs only tests tagged with @Regression
mvn test -Dcucumber.filter.tags="@Regression"

## 📊 Generating Reports
### Generate the Allure report (cleans previous results)
allure generate (target-file name)/allure-results -o target/r

### Open the generated report in your browser
allure open target/allure-report

---

## 🤖 AI Layer (Java-native)

A Claude-powered layer for story → test generation and failure analysis, **with no extra
tool chain required** (plain Java + Gson + JDK HTTP client). Single entry point: `ai.Cli`.

```
User Story ──▶ AI Test Generator ──▶ .feature + step + page object + locator constants
                                          │  (never overwrites existing files)
                                          ▼
                              mvn test ──▶ Cucumber + Appium
                                          │  (reports/cucumber-report.json)
                                          ▼
                  AI Failure Analyzer ──▶ reports/ai-analysis.md
```

**Every piece of text** sent to the LLM first passes through `Redactor` (email/IBAN/card/
Turkish national ID/phone patterns + an optional `ai-secrets.local.txt` denylist). To mask
some secret values verbatim, you can add an `ai-secrets.local.txt` file (one value per line)
at the project root; this file is in `.gitignore`.

### Setup

Lookup order for the API key (`ANTHROPIC_API_KEY`) — any one of these is enough:

1. **`.env` file in the project (recommended):**
   ```bash
   cp .env.example .env
   # in .env: ANTHROPIC_API_KEY=sk-ant-...
   ```
   `.env` is in `.gitignore`, it is never committed.
2. Environment variable: `export ANTHROPIC_API_KEY=sk-ant-...`
3. Maven parameter: `mvn ... -DANTHROPIC_API_KEY=sk-ant-...`

### Generate tests with AI

Generates a feature + step definition + page object + locator constants from a user story
(preserves existing files, leaves a `.generated` sibling file on conflict):

```bash
mvn -q compile exec:java -Dexec.args="generate stories/menu-navigation.example.txt"
# or directly with text:
mvn -q compile exec:java -Dexec.args="generate 'As a user I want to ...'"
```

### AI failure analysis

After running `mvn test`, classifies the failed scenarios in the generated
`reports/cucumber-report.json` report as `app-bug / test-bug / flaky / environment`:

```bash
mvn -q compile exec:java -Dexec.args="analyze"
# custom report path:
mvn -q compile exec:java -Dexec.args="analyze path/to/cucumber-report.json"
```

Output: console summary + `reports/ai-analysis.md`.

---

# Kadir Atalı 🧑‍💻

**📍 QA Automation Engineer**

[💼 LinkedIn](https://www.linkedin.com/in/kadir-atali/) | [🐙 GitHub](https://github.com/kadiratali)

