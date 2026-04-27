(() => {
  const calendar = document.querySelector("#slot-calendar");
  const monthKicker = document.querySelector("#slot-month-kicker");
  const pageFeedback = document.querySelector("#slot-page-feedback");
  const selectedDateLabel = document.querySelector("#slot-selected-date");
  const selectedSummary = document.querySelector("#slot-selected-summary");
  const editorList = document.querySelector("#slot-editor-list");
  const prevMonthButton = document.querySelector("#slot-prev-month");
  const nextMonthButton = document.querySelector("#slot-next-month");
  const reloadButton = document.querySelector("#slot-reload");

  if (!calendar || !editorList) {
    return;
  }

  const statusOptions = [
    { value: "OPEN", label: "受付中" },
    { value: "LIMITED", label: "残少" },
    { value: "FULL", label: "満席" },
    { value: "STOPPED", label: "停止" },
  ];
  const weekdayLabels = ["日", "月", "火", "水", "木", "金", "土"];
  const url = new URL(window.location.href);
  const today = new Date();
  const initialYear = Number(url.searchParams.get("year")) || Number(calendar.dataset.year) || today.getFullYear();
  const initialMonth = Number(url.searchParams.get("month")) || Number(calendar.dataset.month) || today.getMonth() + 1;

  let dayCards = [];
  const state = {
    year: initialYear,
    month: initialMonth,
    selectedDay: today.getFullYear() === initialYear && today.getMonth() + 1 === initialMonth ? today.getDate() : 1,
    monthData: null,
  };

  function setPageFeedback(message, isError = false) {
    if (!pageFeedback) {
      return;
    }
    pageFeedback.textContent = message;
    pageFeedback.style.color = isError ? "#9f403d" : "";
  }

  function getIsoDate(day) {
    return `${state.year}-${String(state.month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function formatMonthText() {
    return `${state.year}年${state.month}月`;
  }

  function formatDateText(isoDate) {
    const date = new Date(`${isoDate}T00:00:00`);
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日（${weekdayLabels[date.getDay()]}）`;
  }

  function normalizeSelectedDay() {
    const lastDay = new Date(state.year, state.month, 0).getDate();
    if (state.selectedDay > lastDay) {
      state.selectedDay = lastDay;
    }
    if (state.selectedDay < 1) {
      state.selectedDay = 1;
    }
  }

  function syncLocation() {
    url.searchParams.set("year", String(state.year));
    url.searchParams.set("month", String(state.month));
    window.history.replaceState({}, "", url);
  }

  function getDayData(day) {
    if (!state.monthData) {
      return null;
    }
    return state.monthData.days.find((entry) => entry.date === getIsoDate(day)) || null;
  }

  function getStatusLabel(status) {
    if (status === "OPEN") return "受付中";
    if (status === "LIMITED") return "残少";
    if (status === "FULL") return "満席";
    if (status === "STOPPED") return "停止";
    if (status === "CLOSED") return "休業";
    return status || "未設定";
  }

  function getLanguageLabel(language) {
    return language === "en" ? "EN" : "JP";
  }

  function getSlotClassName(slot) {
    if (slot.effectiveStatus === "CLOSED") {
      return "admin-slot is-closed";
    }
    if (slot.effectiveStatus === "FULL" || slot.effectiveStatus === "LIMITED") {
      return "admin-slot is-full";
    }
    if (slot.effectiveStatus === "STOPPED") {
      return "admin-slot is-stop";
    }
    return "admin-slot";
  }

  function getHolidayClosedSlots(dayData) {
    if (!dayData) {
      return [];
    }
    return dayData.slots.filter((slot) => slot.effectiveStatus === "CLOSED");
  }

  function getPartialHolidaySummary(dayData) {
    const closedSlots = getHolidayClosedSlots(dayData);
    if (closedSlots.length === 0) {
      return "";
    }

    const languages = [...new Set(closedSlots.map((slot) => slot.guideLanguage))];
    if (languages.length === 2) {
      return "休業";
    }
    if (languages[0] === "en") {
      return "EN休業";
    }
    return "JP休業";
  }

  function getHolidayReasonLabel(dayData) {
    if (!dayData?.holidayReason) {
      return "休業";
    }
    if (dayData.holidayReason === "定休日ルールで休業") {
      return "定休日";
    }
    return dayData.holidayReason;
  }

  function createDayCard(day) {
    const card = document.createElement("button");
    const number = document.createElement("strong");
    const summary = document.createElement("p");
    const slotList = document.createElement("div");

    card.type = "button";
    card.className = "admin-day admin-day-button";
    card.dataset.day = String(day);

    number.className = "admin-day-number";
    number.textContent = String(day);

    summary.className = "slot-day-note";
    slotList.className = "slot-day-list";

    card.append(number, summary, slotList);
    card.addEventListener("click", () => {
      state.selectedDay = day;
      renderCalendar();
      renderSelectedDayEditor();
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
    monthKicker.textContent = formatMonthText();

    dayCards.forEach((card) => {
      const day = Number(card.dataset.day);
      const dayData = getDayData(day);
      const summary = card.querySelector(".slot-day-note");
      const slotList = card.querySelector(".slot-day-list");

      card.classList.toggle("is-selected", day === state.selectedDay);
      card.classList.remove("holiday-day-closed", "holiday-day-special", "holiday-day-partial");
      slotList.replaceChildren();

      if (!dayData) {
        summary.textContent = "枠情報はまだありません。";
        return;
      }

      if (dayData.holidayType === "CLOSED" && dayData.closed) {
        card.classList.add("holiday-day-closed");
      }
      if (dayData.holidayType === "SPECIAL_OPEN") {
        card.classList.add("holiday-day-special");
      }
      if (!dayData.closed && getHolidayClosedSlots(dayData).length > 0) {
        card.classList.add("holiday-day-partial");
      }

      if (dayData.closed) {
        summary.textContent = getHolidayReasonLabel(dayData);
      } else if (dayData.holidayType === "SPECIAL_OPEN") {
        summary.textContent = dayData.holidayReason ? `例外営業: ${dayData.holidayReason}` : "例外営業日";
      } else if (getHolidayClosedSlots(dayData).length > 0) {
        summary.textContent = getPartialHolidaySummary(dayData);
      } else {
        const openCount = dayData.slots.filter((slot) => slot.effectiveStatus === "OPEN" || slot.effectiveStatus === "LIMITED").length;
        summary.textContent = `${openCount}件受付中`;
      }

      dayData.slots.forEach((slot) => {
        const badge = document.createElement("div");
        badge.className = getSlotClassName(slot);
        badge.innerHTML = `<span>${slot.timeSlot} ${getLanguageLabel(slot.guideLanguage)}</span><span>${getStatusLabel(slot.effectiveStatus)}</span>`;
        slotList.append(badge);
      });
    });
  }

  function buildStatusSelect(slot) {
    const select = document.createElement("select");
    select.className = "slot-editor-status";
    select.dataset.slotId = String(slot.id);

    statusOptions.forEach((option) => {
      const element = document.createElement("option");
      element.value = option.value;
      element.textContent = option.label;
      if (slot.slotStatus === option.value) {
        element.selected = true;
      }
      select.append(element);
    });

    if (slot.effectiveStatus === "CLOSED") {
      select.disabled = true;
    }

    return select;
  }

  function buildGuideInput(slot) {
    const input = document.createElement("input");
    input.type = "text";
    input.className = "slot-editor-guide";
    input.value = slot.guideName || "";
    input.dataset.slotId = String(slot.id);
    input.placeholder = slot.guideLanguage === "en" ? "English Guide" : "Japanese Guide";
    if (slot.effectiveStatus === "CLOSED") {
      input.disabled = true;
    }
    return input;
  }

  async function saveSlot(slotId, guideInput, statusSelect, feedback) {
    feedback.textContent = "保存中です。";
    feedback.style.color = "";

    try {
      const response = await fetch(`/api/admin/slots/${slotId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          guideName: guideInput.value,
          slotStatus: statusSelect.value,
        }),
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "枠の更新に失敗しました。");
      }

      feedback.textContent = "保存しました。";
      await loadMonth();
    } catch (error) {
      feedback.textContent = error.message || "枠の更新に失敗しました。";
      feedback.style.color = "#9f403d";
    }
  }

  function renderSelectedDayEditor() {
    const dayData = getDayData(state.selectedDay);
    editorList.replaceChildren();

    if (!dayData) {
      selectedDateLabel.textContent = "日付を選択してください";
      selectedSummary.textContent = "この月の枠データはまだ読み込まれていません。";
      return;
    }

    selectedDateLabel.textContent = formatDateText(dayData.date);
    if (dayData.closed) {
      selectedSummary.textContent = dayData.holidayReason
        ? `この日は休業日です。理由: ${dayData.holidayReason}`
        : "この日は休業日です。slot の effectiveStatus はすべて CLOSED になります。";
    } else if (dayData.holidayType === "SPECIAL_OPEN") {
      selectedSummary.textContent = "例外営業日です。定休日ルールより slot の個別設定を優先します。";
    } else if (getHolidayClosedSlots(dayData).length > 0) {
      selectedSummary.textContent = `${getPartialHolidaySummary(dayData)}です。該当言語の枠は holiday_control により休業扱いです。`;
    } else {
      selectedSummary.textContent = "各枠のステータスと担当ガイドを更新できます。";
    }

    dayData.slots.forEach((slot) => {
      const field = document.createElement("div");
      const label = document.createElement("label");
      const meta = document.createElement("div");
      const guideInput = buildGuideInput(slot);
      const statusSelect = buildStatusSelect(slot);
      const actions = document.createElement("div");
      const saveButton = document.createElement("button");
      const feedback = document.createElement("p");

      field.className = "admin-field slot-editor-row";
      label.className = "slot-editor-label";
      label.textContent = `${slot.timeSlot} ${getLanguageLabel(slot.guideLanguage)}`;

      meta.className = "slot-editor-meta";
      meta.textContent = `現在: ${getStatusLabel(slot.slotStatus)} / 実際の判定: ${getStatusLabel(slot.effectiveStatus)}`;

      actions.className = "slot-editor-actions";

      saveButton.type = "button";
      saveButton.className = "btn btn-secondary";
      saveButton.textContent = "この枠を保存";
      saveButton.disabled = slot.effectiveStatus === "CLOSED";
      saveButton.addEventListener("click", () => saveSlot(slot.id, guideInput, statusSelect, feedback));

      feedback.className = "admin-empty slot-editor-feedback";
      feedback.textContent = slot.effectiveStatus === "CLOSED"
        ? `${getLanguageLabel(slot.guideLanguage)}枠は holiday_control 側で休業です。`
        : "未保存";

      actions.append(statusSelect, saveButton);
      field.append(label, meta, guideInput, actions, feedback);
      editorList.append(field);
    });
  }

  async function loadMonth() {
    normalizeSelectedDay();
    syncLocation();
    setPageFeedback("月間データを読み込んでいます。");

    try {
      const response = await fetch(`/api/admin/slots?year=${state.year}&month=${state.month}`);
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "枠データの取得に失敗しました。");
      }

      state.monthData = await response.json();
      rebuildCalendar();
      renderCalendar();
      renderSelectedDayEditor();
      setPageFeedback(`${formatMonthText()} の枠データを読み込みました。`);
    } catch (error) {
      state.monthData = null;
      rebuildCalendar();
      renderSelectedDayEditor();
      setPageFeedback(error.message || "枠データの取得に失敗しました。", true);
    }
  }

  prevMonthButton?.addEventListener("click", () => {
    state.month -= 1;
    if (state.month < 1) {
      state.month = 12;
      state.year -= 1;
    }
    loadMonth();
  });

  nextMonthButton?.addEventListener("click", () => {
    state.month += 1;
    if (state.month > 12) {
      state.month = 1;
      state.year += 1;
    }
    loadMonth();
  });

  reloadButton?.addEventListener("click", () => {
    loadMonth();
  });

  loadMonth();
})();
