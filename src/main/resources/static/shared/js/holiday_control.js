(() => {
  const calendar = document.querySelector(".holiday-calendar");
  const pageFeedback = document.querySelector("#holiday-page-feedback");
  const selectedDateLabel = document.querySelector("#selected-holiday-date");
  const monthLabel = document.querySelector("#holiday-month-label");
  const calendarTitle = document.querySelector("#holiday-calendar-title");
  const closedSummary = document.querySelector("#holiday-summary-closed");
  const specialSummary = document.querySelector("#holiday-summary-special");
  const allOpenSummary = document.querySelector("#holiday-summary-all-open");
  const allClosedSummary = document.querySelector("#holiday-summary-all-closed");
  const enOpenSummary = document.querySelector("#holiday-summary-en-open");
  const enClosedSummary = document.querySelector("#holiday-summary-en-closed");
  const jaOpenSummary = document.querySelector("#holiday-summary-ja-open");
  const jaClosedSummary = document.querySelector("#holiday-summary-ja-closed");
  const statusSelect = document.querySelector("#holiday-status-select");
  const reasonInput = document.querySelector("#holiday-reason-input");
  const languageSelect = document.querySelector("#holiday-language-select");
  const viewLanguageSelect = document.querySelector("#holiday-view-language-select");
  const saveDayButton = document.querySelector("#holiday-save-day");
  const ruleSelect = document.querySelector("#holiday-repeat-rule");
  const exceptionsInput = document.querySelector("#holiday-open-exceptions");
  const applyRuleButton = document.querySelector("#holiday-apply-rule");
  const prevMonthButton = document.querySelector("#holiday-prev-month");
  const nextMonthButton = document.querySelector("#holiday-next-month");
  const ruleFeedback = document.querySelector("#holiday-rule-feedback");

  if (!calendar) {
    return;
  }

  const url = new URL(window.location.href);
  const today = new Date();
  const initialYear = Number(url.searchParams.get("year")) || Number(calendar.dataset.year) || today.getFullYear();
  const initialMonth = Number(url.searchParams.get("month")) || Number(calendar.dataset.month) || today.getMonth() + 1;

  let dayCards = [];
  const state = {
    year: initialYear,
    month: initialMonth,
    selectedDay: today.getFullYear() === initialYear && today.getMonth() + 1 === initialMonth ? today.getDate() : 1,
    monthlyHolidays: new Map(),
    monthlyScopeRecords: {
      all: [],
      en: [],
      ja: [],
    },
  };

  const weeklyRuleOptions = [
    { value: "SUNDAY", label: "毎週 日曜日を定休日にする" },
    { value: "MONDAY", label: "毎週 月曜日を定休日にする" },
    { value: "TUESDAY", label: "毎週 火曜日を定休日にする" },
    { value: "WEDNESDAY", label: "毎週 水曜日を定休日にする" },
    { value: "THURSDAY", label: "毎週 木曜日を定休日にする" },
    { value: "FRIDAY", label: "毎週 金曜日を定休日にする" },
    { value: "SATURDAY", label: "毎週 土曜日を定休日にする" },
    { value: "NONE", label: "定休日ルールなし" },
  ];

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

  function getViewLanguage() {
    return viewLanguageSelect?.value || "";
  }

  function getIsoDate(day) {
    return `${state.year}-${String(state.month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function getReadableDate(day) {
    return `${state.year}年${state.month}月${day}日`;
  }

  function getHolidayKey(day) {
    return getIsoDate(day);
  }

  function initializeWeeklyRuleOptions() {
    if (!ruleSelect) {
      return;
    }

    const currentValue = ruleSelect.value || "TUESDAY";
    ruleSelect.replaceChildren(
      ...weeklyRuleOptions.map((option) => {
        const element = document.createElement("option");
        element.value = option.value;
        element.textContent = option.label;
        return element;
      })
    );
    ruleSelect.value = weeklyRuleOptions.some((option) => option.value === currentValue) ? currentValue : "TUESDAY";
  }

  function normalizeDateToken(value) {
    return value
      .trim()
      .replace(/[‐‑‒–—―ー－]/g, "-")
      .replace(/[／]/g, "/");
  }

  function parseExceptionDates(raw) {
    return raw
      .split(/[\s,、，]+/)
      .map((value) => normalizeDateToken(value))
      .filter(Boolean)
      .map((value) => value.replace(/\//g, "-"));
  }

  function getTypeLabel(type) {
    if (type === "CLOSED") return "休業日";
    if (type === "SPECIAL_OPEN") return "例外営業";
    return "通常営業";
  }

  function getDisplayReason(record) {
    if (!record) {
      return "";
    }
    if (record.reason === "定休日ルールで休業") {
      return "定休日";
    }
    return record.reason || getTypeLabel(record.holidayType);
  }

  function getLanguageLabel(language) {
    if (language === "en") return "EN";
    if (language === "ja") return "JP";
    return "ALL";
  }

  function syncLocation() {
    url.searchParams.set("year", String(state.year));
    url.searchParams.set("month", String(state.month));
    const language = getViewLanguage();
    if (language) {
      url.searchParams.set("language", language);
    } else {
      url.searchParams.delete("language");
    }
    window.history.replaceState({}, "", url);
  }

  function updateMonthHeading() {
    const monthText = `${state.year}年${state.month}月`;
    if (monthLabel) {
      monthLabel.textContent = monthText;
    }
    if (calendarTitle) {
      calendarTitle.textContent = `${monthText}の休業日カレンダー`;
    }
  }

  function updateSummary() {
    let closedCount = 0;
    let specialCount = 0;

    state.monthlyHolidays.forEach((record) => {
      if (record.holidayType === "CLOSED") {
        closedCount += 1;
      }
      if (record.holidayType === "SPECIAL_OPEN") {
        specialCount += 1;
      }
    });

    if (closedSummary) {
      closedSummary.textContent = `${closedCount}日`;
    }
    if (specialSummary) {
      specialSummary.textContent = `${specialCount}日`;
    }
  }

  function getDaysInMonth() {
    return new Date(state.year, state.month, 0).getDate();
  }

  function countClosedDates(records, mode) {
    const closedDates = new Set();

    records.forEach((record) => {
      if (record.holidayType !== "CLOSED") {
        return;
      }
      if (mode === "shared" && record.appliesToLanguage !== null) {
        return;
      }
      closedDates.add(record.holidayDate);
    });

    return closedDates.size;
  }

  function updateOperatingSummary() {
    const totalDays = getDaysInMonth();
    const sharedClosed = countClosedDates(state.monthlyScopeRecords.all, "shared");
    const enClosed = countClosedDates(state.monthlyScopeRecords.en, "language");
    const jaClosed = countClosedDates(state.monthlyScopeRecords.ja, "language");

    if (allOpenSummary) {
      allOpenSummary.textContent = `${totalDays - sharedClosed}日`;
    }
    if (allClosedSummary) {
      allClosedSummary.textContent = `${sharedClosed}日`;
    }
    if (enOpenSummary) {
      enOpenSummary.textContent = `${totalDays - enClosed}日`;
    }
    if (enClosedSummary) {
      enClosedSummary.textContent = `${enClosed}日`;
    }
    if (jaOpenSummary) {
      jaOpenSummary.textContent = `${totalDays - jaClosed}日`;
    }
    if (jaClosedSummary) {
      jaClosedSummary.textContent = `${jaClosed}日`;
    }
  }

  function updateSelectedDayIfOutOfRange() {
    const lastDay = new Date(state.year, state.month, 0).getDate();
    if (state.selectedDay > lastDay) {
      state.selectedDay = lastDay;
    }
  }

  function createDayCard(day) {
    const card = document.createElement("div");
    const number = document.createElement("strong");
    const note = document.createElement("p");

    card.className = "admin-day";
    card.dataset.day = String(day);

    number.className = "admin-day-number";
    number.textContent = String(day);

    note.className = "holiday-day-note";
    note.textContent = "通常営業";

    card.append(number, note);
    card.addEventListener("click", () => {
      state.selectedDay = day;
      renderCalendar();
      fillSelectedDayForm();
    });

    return card;
  }

  function createPlaceholderCard() {
    const placeholder = document.createElement("div");
    placeholder.className = "admin-day is-placeholder";
    placeholder.setAttribute("aria-hidden", "true");
    return placeholder;
  }

  function rebuildCalendar() {
    const firstDay = new Date(state.year, state.month - 1, 1).getDay();
    const lastDay = new Date(state.year, state.month, 0).getDate();

    calendar.querySelectorAll(".admin-day").forEach((node) => node.remove());

    for (let index = 0; index < firstDay; index += 1) {
      calendar.append(createPlaceholderCard());
    }

    dayCards = [];
    for (let day = 1; day <= lastDay; day += 1) {
      const card = createDayCard(day);
      dayCards.push(card);
      calendar.append(card);
    }
  }

  function renderCalendar() {
    dayCards.forEach((card) => {
      const day = Number(card.dataset.day);
      const note = card.querySelector(".holiday-day-note");
      const record = state.monthlyHolidays.get(getHolidayKey(day));

      card.classList.remove("holiday-day-closed", "is-selected", "holiday-day-special");

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
        const scopeText = record.appliesToLanguage ? ` (${getLanguageLabel(record.appliesToLanguage)})` : "";
        note.textContent = `${getDisplayReason(record)}${scopeText}`;
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

  async function fetchMonthlyHolidaysByScope(scope) {
    const params = new URLSearchParams({
      year: String(state.year),
      month: String(state.month),
    });
    if (scope) {
      params.set("language", scope);
    }

    const response = await fetch(`/api/admin/holidays?${params.toString()}`);
    if (!response.ok) {
      throw new Error("休業日データの取得に失敗しました。");
    }
    return response.json();
  }

  async function loadMonthlyHolidays() {
    updateMonthHeading();
    updateSelectedDayIfOutOfRange();
    rebuildCalendar();
    syncLocation();

    const language = getViewLanguage();
    const scopeText = getLanguageLabel(language);
    setPageFeedback(`${state.year}年${state.month}月の休業日データを読み込んでいます。`);

    const [allHolidays, enHolidays, jaHolidays] = await Promise.all([
      fetchMonthlyHolidaysByScope(""),
      fetchMonthlyHolidaysByScope("en"),
      fetchMonthlyHolidaysByScope("ja"),
    ]);

    state.monthlyScopeRecords = {
      all: allHolidays,
      en: enHolidays,
      ja: jaHolidays,
    };

    const holidays = language === "en"
      ? enHolidays
      : language === "ja"
        ? jaHolidays
        : allHolidays;

    state.monthlyHolidays = new Map(
      holidays.map((holiday) => [holiday.holidayDate, holiday])
    );

    renderCalendar();
    fillSelectedDayForm();
    updateSummary();
    updateOperatingSummary();
    setPageFeedback(`${state.year}年${state.month}月の${scopeText}データを読み込みました。`);
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
        throw new Error(errorBody.message || "休業日の登録に失敗しました。");
      }
    }

    await loadMonthlyHolidays();
    setPageFeedback(`${getReadableDate(state.selectedDay)} の設定を保存しました。`);
  }

  async function applyRule() {
    const weeklyClosedDay = ruleSelect.value;

    if (weeklyClosedDay === "NONE") {
      setRuleFeedback("ルールなしを選んだ場合は一括適用を行いません。必要な日だけ個別に保存してください。");
      return;
    }

    setRuleFeedback("定休日ルールを保存しています。");

    const response = await fetch("/api/admin/holidays/apply-rule", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        year: state.year,
        month: state.month,
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
    setRuleFeedback(`定休日ルールを保存しました。対象日数: ${result.length}日`);
  }

  function moveMonth(offset) {
    const current = new Date(state.year, state.month - 1, 1);
    current.setMonth(current.getMonth() + offset);
    state.year = current.getFullYear();
    state.month = current.getMonth() + 1;
    loadMonthlyHolidays().catch((error) => {
      setPageFeedback(error.message || "月間データの取得に失敗しました。", true);
    });
  }

  const initialLanguage = url.searchParams.get("language");
  if (initialLanguage && viewLanguageSelect) {
    viewLanguageSelect.value = initialLanguage;
  }

  initializeWeeklyRuleOptions();

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

  viewLanguageSelect?.addEventListener("change", () => {
    loadMonthlyHolidays().catch((error) => {
      setPageFeedback(error.message || "月間データの取得に失敗しました。", true);
    });
  });

  prevMonthButton?.addEventListener("click", () => moveMonth(-1));
  nextMonthButton?.addEventListener("click", () => moveMonth(1));

  loadMonthlyHolidays().catch((error) => {
    setPageFeedback(error.message || "月間データの取得に失敗しました。", true);
  });
})();
