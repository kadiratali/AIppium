# 🚀 Appium QA Automation Framework

> **A modern, scalable, and reusable Appium-based QA automation framework for mobile testing.**

## 🧩 Overview

This framework has been developed to accelerate test processes, ensure reusability, and detect issues early in mobile applications.  
It supports both **Android** and **iOS** platforms and provides an easy way to manage and execute automated mobile tests.

### ✨ Key Features
- 🌐 Cross-platform support (Android & iOS)
- 🧠 Modular structure using **Page Object Model (POM)**
- ⚙️ Parallel test execution support
- 📸 Automatic screenshot and screen recording capture on failure
- 🩹 Self-healing locators: broken locators are re-resolved from the page source using last-known-good attributes (configurable via `selfHealing` in `src/config.json`; every heal is logged and attached to the Allure report)
- 🤖 AI-powered assertions (`assertAi`) for perceptual/derived-data checks a locator can't express, with human-readable failure reasons
- 🖥️ A local dashboard to trigger CI runs by tag, watch them live, and see the Allure report inline
- 🧾 Detailed reporting with **Allure**
- 🧪 Optional **Cucumber BDD** integration

---

## 🏗️ Tech Stack

| Technology     | Description |
|----------------|-------------|
| **Java 21**    | Core programming language |
| **Appium 3.x** | Mobile automation framework |
| **TestNG**     | Test execution framework |
| **Maven**      | Build and dependency management |
| **Allure**     | Test reporting |
| **SLF4J**      | Logging utility |
| **GitHub Actions** | CI: build, emulator-based test run, artifact reporting |
| **Anthropic Claude API** | AI assertions (`assertAi`) |

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

## 🤖 AI Assertions (`assertAi`)

For checks that are hard to express with a locator - perceptual ones ("no overlapping text", "an error banner is shown") or ones derived from on-screen data (sums, ordering, format consistency) - `ctx.asserts.assertAi(String expectation)` sends the current screenshot + page source to Claude and asserts on its verdict:

```java
ctx.asserts.assertAi("A context menu with two options, 'Menu A' and 'Menu B', is visible on screen.");
```

- On failure, the assertion message is the model's own explanation of what was expected vs. what's actually on screen (quoting real texts/resource-ids) - not a stack trace, so a reader doesn't need to open the device to understand what broke.
- Every evaluation (pass **or** fail) is attached to the Allure report as "AI assertion", so it stays auditable even when it passes.
- For exact/deterministic values, prefer the regular locator-based asserts - AI verdicts aren't guaranteed reproducible.
- Fails loudly (never silently passes) if `ANTHROPIC_API_KEY` is missing or the API call errors.

**Setup:** add `ANTHROPIC_API_KEY=sk-ant-...` to a `.env` file at the project root (`cp .env.example .env`), or export it as an environment variable.

---

## 🖥️ Test Runner Dashboard

A local, retro-styled web dashboard for running scenarios by tag without touching a terminal:

```bash
mvn -q compile exec:java
```

Then open **http://localhost:8090**, type a tag expression (e.g. `@Regression`, `@ContextMenu`, `@Smoke and not @Flaky`), and hit **RUN**.

- Runs always go through **CI** (GitHub Actions, a real emulator on a GitHub-hosted runner) instead of locally, so everyone using the dashboard gets the same environment. It dispatches the `CI` workflow's `workflow_dispatch` trigger with the given tag.
- Job/step status streams live into a log panel while the run is in progress.
- Once the run finishes, the dashboard downloads the `appium-test-reports` artifact, shows a pass/fail scenario table, and renders the generated Allure report inline.
- **Requires:**
  - A `GITHUB_TOKEN` (`repo` + `workflow` scope) in `.env` - used to dispatch the workflow, poll its status, and download the resulting artifact.
  - The `allure` CLI on `PATH` - used to generate the HTML report from the downloaded results.

---

# Kadir Atalı 🧑‍💻

**📍 QA Automation Engineer**

[💼 LinkedIn](https://www.linkedin.com/in/kadir-atali/) | [🐙 GitHub](https://github.com/kadiratali)

