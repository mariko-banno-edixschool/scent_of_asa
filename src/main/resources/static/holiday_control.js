(() => {
  const calendar = document.querySelector(".holiday-calendar");
  const ruleSelect = document.querySelector("#holiday-repeat-rule");
  const exceptionsInput = document.querySelector("#holiday-open-exceptions");
  const applyButton = document.querySelector("#holiday-apply-rule");
  const feedback = document.querySelector("#holiday-rule-feedback");

  if (!calendar || !ruleSelect || !exceptionsInput || !applyButton) {
    return;
  }

  const year = Number(calendar.dataset.year);
  const month = Number(calendar.dataset.month);
  const dayCards = [...calendar.querySelectorAll(".admin-day[data-day]")];

  dayCards.forEach((card) => {
    const note = card.querySelector(".holiday-day-note");
    card.dataset.baseNote = note ? note.textContent.trim() : "";
  });

  function parseExceptionDays(raw) {
    return new Set(
      raw
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean)
        .map((value) => {
          const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
          if (!match) {
            return null;
          }

          const [, y, m, d] = match;
          if (Number(y) !== year || Number(m) !== month) {
            return null;
          }

          return Number(d);
        })
        .filter((value) => Number.isInteger(value))
    );
  }

  function isRuleClosed(day, ruleText) {
    const weekday = new Date(year, month - 1, day).getDay();

    if (ruleText.includes("火曜日")) {
      return weekday === 2;
    }

    if (ruleText.includes("水曜日")) {
      return weekday === 3;
    }

    return false;
  }

  function updateCalendarFromRule() {
    const ruleText = ruleSelect.value;
    const exceptions = parseExceptionDays(exceptionsInput.value);
    let appliedCount = 0;

    dayCards.forEach((card) => {
      const day = Number(card.dataset.day);
      const note = card.querySelector(".holiday-day-note");
      const isManualClosed = card.dataset.manualClosed === "true";
      const closedByRule = isRuleClosed(day, ruleText) && !exceptions.has(day);

      if (isManualClosed) {
        card.classList.add("holiday-day-closed");
        if (note) {
          note.textContent = card.dataset.baseNote;
        }
        return;
      }

      if (closedByRule) {
        card.classList.add("holiday-day-closed");
        if (note) {
          note.textContent = "定休日ルールで休業";
        }
        appliedCount += 1;
      } else {
        card.classList.remove("holiday-day-closed");
        if (note) {
          note.textContent = "通常営業";
        }
      }
    });

    if (feedback) {
      if (ruleText.includes("定休日なし")) {
        feedback.textContent = "定休日ルールは解除され、手動で設定した休業日だけが残っています。";
      } else {
        feedback.textContent = `定休日ルールを反映しました。自動で休業になった日: ${appliedCount}日`;
      }
    }
  }

  applyButton.addEventListener("click", updateCalendarFromRule);
  ruleSelect.addEventListener("change", updateCalendarFromRule);
  exceptionsInput.addEventListener("change", updateCalendarFromRule);
})();
