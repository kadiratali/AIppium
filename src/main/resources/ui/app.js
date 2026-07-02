const tagInput = document.getElementById("tag");
const runBtn = document.getElementById("runBtn");
const statusLine = document.getElementById("statusLine");
const runLink = document.getElementById("runLink");
const runner = document.getElementById("runner");
const progressBar = document.getElementById("progressBar");
const logOutput = document.getElementById("logOutput");
const resultsSection = document.getElementById("resultsSection");
const resultsTable = document.getElementById("results");
const resultsBody = resultsTable.querySelector("tbody");
const allureSection = document.getElementById("allureSection");
const allureFrame = document.getElementById("allureFrame");

const BAR_WIDTH = 20;
let barFrame = 0;
let barTimer = null;
let statusTimer = null;
let eventSource = null;

runBtn.addEventListener("click", startRun);
tagInput.addEventListener("keydown", (e) => {
  if (e.key === "Enter") startRun();
});

async function startRun() {
  const tag = tagInput.value.trim();
  if (!tag) {
    statusLine.textContent = "ERROR: enter a tag expression first.";
    return;
  }

  const res = await fetch("/api/run", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ tag }),
  });
  const body = await res.json();

  if (!res.ok) {
    statusLine.textContent = "ERROR: " + body.error;
    return;
  }

  runBtn.disabled = true;
  resultsSection.classList.add("hidden");
  allureSection.classList.add("hidden");
  runLink.classList.add("hidden");
  runner.classList.remove("hidden");
  logOutput.textContent = "";
  statusLine.textContent = "DISPATCHING CI RUN FOR: " + tag;

  startProgressBar();
  connectLogStream();
  statusTimer = setInterval(pollStatus, 1500);
}

function startProgressBar() {
  barFrame = 0;
  clearInterval(barTimer);
  barTimer = setInterval(() => {
    barFrame = (barFrame + 1) % (BAR_WIDTH + 1);
    progressBar.textContent = "[" + "■".repeat(barFrame) + "□".repeat(BAR_WIDTH - barFrame) + "]";
  }, 150);
}

function stopProgressBar() {
  clearInterval(barTimer);
  progressBar.textContent = "[" + "■".repeat(BAR_WIDTH) + "]";
}

function connectLogStream() {
  if (eventSource) {
    eventSource.close();
  }
  eventSource = new EventSource("/api/logs/stream");
  eventSource.onmessage = (e) => {
    logOutput.textContent += e.data + "\n";
    logOutput.scrollTop = logOutput.scrollHeight;
  };
  eventSource.addEventListener("done", () => {
    eventSource.close();
    eventSource = null;
  });
  eventSource.onerror = () => {
    // Connection dropped (e.g. server restarted) - stop retrying silently.
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
  };
}

async function pollStatus() {
  const res = await fetch("/api/status");
  const body = await res.json();
  if (!body.running) {
    clearInterval(statusTimer);
    stopProgressBar();
    runner.classList.add("hidden");
    runBtn.disabled = false;
    statusLine.textContent = "FINISHED.";
    await loadResults();
  }
}

async function loadResults() {
  const res = await fetch("/api/results");
  const body = await res.json();

  if (body.runUrl) {
    runLink.innerHTML = "CI run: <a href=\"" + body.runUrl + "\" target=\"_blank\">" + body.runUrl + "</a>";
    runLink.classList.remove("hidden");
  }

  if (body.error) {
    statusLine.textContent = "ERROR: " + body.error;
  }

  resultsBody.innerHTML = "";
  for (const scenario of body.scenarios) {
    const row = document.createElement("tr");

    const statusCell = document.createElement("td");
    statusCell.textContent = scenario.passed ? "PASS" : "FAIL";
    statusCell.className = scenario.passed ? "status-pass" : "status-fail";

    const featureCell = document.createElement("td");
    featureCell.textContent = scenario.feature;

    const scenarioCell = document.createElement("td");
    scenarioCell.textContent = scenario.scenario;

    const errorCell = document.createElement("td");
    errorCell.textContent = scenario.passed ? "" : `${scenario.failedStep || ""} ${scenario.errorMessage || ""}`.trim();

    row.append(statusCell, featureCell, scenarioCell, errorCell);
    resultsBody.appendChild(row);
  }
  if (body.scenarios.length > 0) {
    resultsSection.classList.remove("hidden");
  }

  if (body.allureReady) {
    allureFrame.src = "/allure/index.html?t=" + Date.now();
    allureSection.classList.remove("hidden");
  }
}
