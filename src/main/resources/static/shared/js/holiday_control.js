(() => {
  const calendar = document.querySelector(".holiday-calendar");
  const pageFeedback = document.querySelector("#holiday-page-feedback");
  const selectedDateLabel = document.querySelector("#selected-holiday-date");
  const statusSelect = document.querySelector("#holiday-status-select");
  const reasonInput = document.querySelector("#holiday-reason-input");
  const languageSelect = document.querySelector("#holiday-language-select");
  const saveDayButton = document.querySelector("#holiday-save-day");
  const ruleSelect = document.querySelector("#holiday-repeat-rule");
  const exceptionsInput = document.querySelector("#holiday-open-exceptions");
  const applyRuleButton = document.querySelector("#holiday-apply-rule");
  const ruleFeedback = document.querySelector("#holiday-rule-feedback");

  if (!calendar) {
    return;
  }

  const year = Number(calendar.dataset.year);
  const month = Number(calendar.dataset.month);
  const dayCards = [...calendar.querySelectorAll(".admin-day[data-day]")];

  const state = {
    selectedDay: 1,
    monthlyHolidays: new Map(),
  };

  function setPageFeedback(message, isError = false) {
    if (!pageFeedback) {
      return;
    }
    pageFeedback.textContent = message;
    pageFeedback.style.color = isError ? "#9f403d" : "";
  }

  function setRuleFeedback(message, isError = false) {
    if (!ruleFeedback) {
      return;
    }
    ruleFeedback.textContent = message;
    ruleFeedback.style.color = isError ? "#9f403d" : "";
  }

  function getIsoDate(day) {
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function getReadableDate(day) {
    return `${year}年${month}月${day}日`;
  }

  function getHolidayKey(day) {
    return getIsoDate(day);
  }

  function parseExceptionDates(raw) {
    return raw
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean);
  }

  function getTypeLabel(type) {
    if (type === "CLOSED") return "休業日";
    if (type === "SPECIAL_OPEN") return "特別営業";
    return "通常営業";
  }

  function renderCalendar() {
    dayCards.forEach((card) => {
      const day = Number(card.dataset.day);
      const note = card.querySelector(".holiday-day-note");
      const record = state.monthlyHolidays.get(getHolidayKey(day));

      card.classList.remove("holiday-day-closed", "is-selected");
      card.classList.remove("holiday-day-special");

      if (day === state.selectedDay) {
        card.classList.add("is-selected");
      }

      if (!record) {
        if (note) {
          note.textContent = "通常営業";
        }
        return;
      }

      if (record.holidayType === "CLOSED") {
        card.classList.add("holiday-day-closed");
      }

      if (record.holidayType === "SPECIAL_OPEN") {
        card.classList.add("holiday-day-special");
      }

      if (note) {
        note.textContent = record.reason || getTypeLabel(record.holidayType);
      }
    });
  }

  function fillSelectedDayForm() {
    const selectedKey = getHolidayKey(state.selectedDay);
    const record = state.monthlyHolidays.get(selectedKey);

    if (selectedDateLabel) {
      selectedDateLabel.textContent = getReadableDate(state.selectedDay);
    }

    if (!record) {
      statusSelect.value = "NORMAL";
      reasonInput.value = "";
      languageSelect.value = "";
      return;
    }

    statusSelect.value = record.holidayType || "CLOSED";
    reasonInput.value = record.reason || "";
    languageSelect.value = record.appliesToLanguage || "";
  }

  async function loadMonthlyHolidays() {
    setPageFeedback("月間データを読み込んでいます。");

    const response = await fetch(`/api/admin/holidays?year=${year}&month=${month}`);
    if (!response.ok) {
      throw new Error("休業日データの取得に失敗しました。");
    }

    const holidays = await response.json();
    state.monthlyHolidays = new Map(
      holidays.map((holiday) => [holiday.holidayDate, holiday])
    );

    renderCalendar();
    fillSelectedDayForm();
    setPageFeedback("DB から月間休業日データを読み込みました。");
  }

  async function saveSelectedDay() {
    const key = getHolidayKey(state.selectedDay);
    const existing = state.monthlyHolidays.get(key);
    const status = statusSelect.value;
    const payload = {
      holidayDate: key,
      holidayType: status,
      reason: reasonInput.value.trim() || null,
      appliesToLanguage: languageSelect.value || null,
      createdByStaffId: 1,
    };

    setPageFeedback(`${getReadableDate(state.selectedDay)} を保存しています。`);

    if (status === "NORMAL") {
      if (existing?.id) {
        const deleteResponse = await fetch(`/api/admin/holidays/${existing.id}`, {
          method: "DELETE",
        });
        if (!deleteResponse.ok) {
          throw new Error("通常営業への戻しに失敗しました。");
        }
      }
    } else if (existing?.id) {
      const updateResponse = await fetch(`/api/admin/holidays/${existing.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!updateResponse.ok) {
        const errorBody = await updateResponse.json().catch(() => ({}));
        throw new Error(errorBody.message || "休業日更新に失敗しました。");
      }
    } else {
      const createResponse = await fetch("/api/admin/holidays", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!createResponse.ok) {
        const errorBody = await createResponse.json().catch(() => ({}));
        throw new Error(errorBody.message || "休業日作成に失敗しました。");
      }
    }

    await loadMonthlyHolidays();
    setPageFeedback(`${getReadableDate(state.selectedDay)} の設定を保存しました。`);
  }

  async function applyRule() {
    const weeklyClosedDay = ruleSelect.value;

    if (weeklyClosedDay === "NONE") {
      setRuleFeedback("定休日なしを選んだ場合、一括反映は行いません。必要な日だけ個別に保存してください。");
      return;
    }

    setRuleFeedback("曜日ルールを保存しています。");

    const response = await fetch("/api/admin/holidays/apply-rule", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        year,
        month,
        weeklyClosedDay,
        reason: "定休日ルールで休業",
        appliesToLanguage: null,
        createdByStaffId: 1,
        openExceptionDates: parseExceptionDates(exceptionsInput.value),
      }),
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.message || "定休日ルールの適用に失敗しました。");
    }

    const result = await response.json();
    await loadMonthlyHolidays();
    setRuleFeedback(`定休日ルールを保存しました。反映対象: ${result.length}日`);
  }

  dayCards.forEach((card) => {
    card.addEventListener("click", () => {
      state.selectedDay = Number(card.dataset.day);
      renderCalendar();
      fillSelectedDayForm();
    });
  });

  saveDayButton?.addEventListener("click", async () => {
    try {
      await saveSelectedDay();
    } catch (error) {
      setPageFeedback(error.message || "保存に失敗しました。", true);
    }
  });

  applyRuleButton?.addEventListener("click", async () => {
    try {
      await applyRule();
    } catch (error) {
      setRuleFeedback(error.message || "ルール適用に失敗しました。", true);
    }
  });

  loadMonthlyHolidays().catch((error) => {
    setPageFeedback(error.message || "月間データの取得に失敗しました。", true);
  });
})();
