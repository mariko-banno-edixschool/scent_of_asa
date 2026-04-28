(() => {
  const storageKey = "scent_of_asa_guide_login_id";
  const calendar = document.querySelector("#guide-calendar");
  const monthKicker = document.querySelector("#guide-month-kicker");
  const pageFeedback = document.querySelector("#guide-page-feedback");
  const selectedDateLabel = document.querySelector("#guide-selected-date");
  const selectedSummary = document.querySelector("#guide-selected-summary");
  const editorList = document.querySelector("#guide-editor-list");
  const prevMonthButton = document.querySelector("#guide-prev-month");
  const nextMonthButton = document.querySelector("#guide-next-month");
  const reloadButton = document.querySelector("#guide-reload");
  const guideSelector = document.querySelector("#guide-selector");
  const guideSwitchButton = document.querySelector("#guide-switch");
  const profileName = document.querySelector("#guide-profile-name");
  const profileMeta = document.querySelector("#guide-profile-meta");

  if (!calendar || !editorList) {
    return;
  }

  const weekdayLabels = ["日", "月", "火", "水", "木", "金", "土"];
  const statusOptions = [
    { value: "OPEN", label: "受付中" },
    { value: "LIMITED", label: "残少" },
    { value: "FULL", label: "満席" },
    { value: "STOPPED", label: "停止" },
  ];

  const url = new URL(window.location.href);
  const today = new Date();
  const initialYear = Number(url.searchParams.get("year")) || Number(calendar.dataset.year) || today.getFullYear();
  const initialMonth = Number(url.searchParams.get("month")) || Number(calendar.dataset.month) || today.getMonth() + 1;
  const initialLoginId = url.searchParams.get("loginId") || window.localStorage.getItem(storageKey) || "";

  let dayCards = [];
  const state = {
    year: initialYear,
    month: initialMonth,
    selectedDay: today.getFullYear() === initialYear && today.getMonth() + 1 === initialMonth ? today.getDate() : 1,
    loginId: initialLoginId,
    guideProfile: null,
    monthData: null,
    guideList: [],
  };

  function setPageFeedback(message, isError = false) {
    if (!pageFeedback) {
      return;
    }
    pageFeedback.textContent = message;
    pageFeedback.style.color = isError ? "#9f403d" : "";
  }

  function syncLocation() {
    url.searchParams.set("year", String(state.year));
    url.searchParams.set("month", String(state.month));
    if (state.loginId) {
      url.searchParams.set("loginId", state.loginId);
      window.localStorage.setItem(storageKey, state.loginId);
    } else {
      url.searchParams.delete("loginId");
      window.localStorage.removeItem(storageKey);
    }
    window.history.replaceState({}, "", url);
  }

  function getIsoDate(day) {
    return `${state.year}-${String(state.month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  function getDayData(day) {
    return state.monthData?.days?.find((entry) => entry.date === getIsoDate(day)) || null;
  }

  function getLanguageLabel(language) {
    return language === "en" ? "EN" : "JP";
  }

  function getStatusLabel(status) {
    if (status === "OPEN") return "受付中";
    if (status === "LIMITED") return "残少";
    if (status === "FULL") return "満席";
    if (status === "STOPPED") return "停止";
    if (status === "CLOSED") return "休業";
    return status || "未設定";
  }

  function getHolidayReasonLabel(dayData) {
    if (!dayData?.holidayReason) {
      return "休業";
    }
    return dayData.holidayReason === "定休日ルールで休業" ? "定休日" : dayData.holidayReason;
  }

  function formatMonthText() {
    return `${state.year}年${state.month}月`;
  }

  function formatDateText(isoDate) {
    const date = new Date(`${isoDate}T00:00:00`);
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 (${weekdayLabels[date.getDay()]})`;
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

  function updateProfile() {
    if (!state.guideProfile) {
      profileName.textContent = "ガイド未選択";
      profileMeta.textContent = "ログインIDを選ぶと今月の担当枠を確認できます。";
      return;
    }

    const languageLabel = getLanguageLabel(state.guideProfile.guideLanguage);
    profileName.textContent = state.guideProfile.displayName;
    profileMeta.textContent = `${languageLabel} / ${state.guideProfile.loginId}`;
  }

  function populateGuideSelector() {
    if (!guideSelector) {
      return;
    }

    guideSelector.innerHTML = '<option value="">ガイドを選択してください</option>';
    state.guideList.forEach((guide) => {
      const option = document.createElement("option");
      option.value = guide.loginId;
      option.textContent = `${guide.displayName} (${getLanguageLabel(guide.guideLanguage)})`;
      option.selected = guide.loginId === state.loginId;
      guideSelector.append(option);
    });
  }

  async function loadGuideList() {
    const response = await fetch("/api/admin/guide-staff");
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "ガイド一覧の取得に失敗しました。");
    }
    state.guideList = await response.json();
    populateGuideSelector();
  }

  async function loadCurrentGuide() {
    if (!state.loginId) {
      state.guideProfile = null;
      updateProfile();
      return;
    }

    const response = await fetch(`/api/guide/me?loginId=${encodeURIComponent(state.loginId)}`);
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "ガイド情報の取得に失敗しました。");
    }
    state.guideProfile = await response.json();
    updateProfile();
  }

  function createPlaceholderCard() {
    const placeholder = document.createElement("div");
    placeholder.className = "admin-day is-placeholder";
    placeholder.setAttribute("aria-hidden", "true");
    return placeholder;
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

  function getSlotClassName(slot, isMine) {
    if (slot.effectiveStatus === "CLOSED") {
      return "admin-slot is-closed";
    }
    if (slot.effectiveStatus === "FULL" || slot.effectiveStatus === "LIMITED") {
      return "admin-slot is-full";
    }
    if (!slot.guideStaffId) {
      return "admin-slot is-stop";
    }
    if (isMine) {
      return "admin-slot guide-slot-mine";
    }
    return "admin-slot guide-slot-other";
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
        summary.textContent = "データ未取得";
        return;
      }

      if (dayData.closed) {
        card.classList.add("holiday-day-closed");
        summary.textContent = getHolidayReasonLabel(dayData);
      } else if (dayData.holidayType === "SPECIAL_OPEN") {
        card.classList.add("holiday-day-special");
        summary.textContent = "例外営業日";
      } else {
        const mineCount = dayData.slots.filter((slot) => slot.guideStaffId === state.guideProfile?.id).length;
        const openCount = dayData.slots.filter((slot) => !slot.guideStaffId && slot.effectiveStatus !== "CLOSED").length;
        if (mineCount > 0) {
          summary.textContent = `${mineCount}枠担当中`;
        } else if (openCount > 0) {
          summary.textContent = `${openCount}枠登録可能`;
        } else {
          summary.textContent = "他ガイド担当済み";
        }
      }

      dayData.slots.forEach((slot) => {
        const badge = document.createElement("div");
        const isMine = slot.guideStaffId === state.guideProfile?.id;
        const rightLabel = slot.effectiveStatus === "CLOSED"
          ? "休業"
          : isMine
            ? "自分"
            : slot.guideName || getStatusLabel(slot.effectiveStatus);
        badge.className = getSlotClassName(slot, isMine);
        badge.innerHTML = `<span>${slot.timeSlot} ${getLanguageLabel(slot.guideLanguage)}</span><span>${rightLabel}</span>`;
        slotList.append(badge);
      });
    });
  }

  function buildStatusSelect(slot, isMine) {
    const select = document.createElement("select");
    select.className = "slot-editor-status";
    const defaultStatus = !slot.guideStaffId && slot.slotStatus === "STOPPED" ? "OPEN" : slot.slotStatus;

    statusOptions.forEach((option) => {
      const element = document.createElement("option");
      element.value = option.value;
      element.textContent = option.label;
      if (defaultStatus === option.value) {
        element.selected = true;
      }
      select.append(element);
    });

    if (slot.effectiveStatus === "CLOSED" || (!isMine && slot.guideStaffId)) {
      select.disabled = true;
    }
    return select;
  }

  async function updateOwnSlot(slot, assigned, statusValue, feedback) {
    feedback.textContent = "更新しています。";
    feedback.style.color = "";

    try {
      const response = await fetch(`/api/guide/slots/${slot.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          loginId: state.loginId,
          assigned,
          slotStatus: assigned ? statusValue : "STOPPED",
        }),
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "枠更新に失敗しました。");
      }

      feedback.textContent = assigned ? "担当枠を更新しました。" : "担当を外しました。";
      await loadMonth();
    } catch (error) {
      feedback.textContent = error.message || "枠更新に失敗しました。";
      feedback.style.color = "#9f403d";
    }
  }

  function renderSelectedDayEditor() {
    const dayData = getDayData(state.selectedDay);
    editorList.replaceChildren();

    if (!state.loginId) {
      selectedDateLabel.textContent = "ガイドを選択してください";
      selectedSummary.textContent = "右側のガイド切替から本人のログインIDを選ぶと、担当可能枠を編集できます。";
      return;
    }

    if (!dayData) {
      selectedDateLabel.textContent = "日付を選択してください";
      selectedSummary.textContent = "この月のデータはまだ読み込まれていません。";
      return;
    }

    selectedDateLabel.textContent = formatDateText(dayData.date);
    if (dayData.closed) {
      selectedSummary.textContent = `この日は ${getHolidayReasonLabel(dayData)} のため、すべての枠が休業です。`;
    } else if (dayData.holidayType === "SPECIAL_OPEN") {
      selectedSummary.textContent = "例外営業日です。担当可能な枠だけ自分で登録してください。";
    } else {
      selectedSummary.textContent = "自分が入る枠だけ登録してください。他ガイドが担当済みの枠は変更できません。";
    }

    dayData.slots.forEach((slot) => {
      const isMine = slot.guideStaffId === state.guideProfile?.id;
      const isAssignedToOther = !!slot.guideStaffId && !isMine;
      const field = document.createElement("div");
      const label = document.createElement("label");
      const meta = document.createElement("div");
      const actions = document.createElement("div");
      const statusSelect = buildStatusSelect(slot, isMine);
      const actionButton = document.createElement("button");
      const feedback = document.createElement("p");

      field.className = "admin-field slot-editor-row";
      label.className = "slot-editor-label";
      label.textContent = `${slot.timeSlot} ${getLanguageLabel(slot.guideLanguage)}`;

      meta.className = "slot-editor-meta";
      if (slot.effectiveStatus === "CLOSED") {
        meta.textContent = `休業中 / 理由: ${getHolidayReasonLabel(dayData)}`;
      } else if (isMine) {
        meta.textContent = `現在: ${getStatusLabel(slot.slotStatus)} / 担当: 自分`;
      } else if (isAssignedToOther) {
        meta.textContent = `現在: ${getStatusLabel(slot.slotStatus)} / 担当: ${slot.guideName}`;
      } else {
        meta.textContent = `現在: ${getStatusLabel(slot.slotStatus)} / 担当未設定`;
      }

      actions.className = "slot-editor-actions";

      actionButton.type = "button";
      actionButton.className = "btn btn-secondary";

      if (slot.effectiveStatus === "CLOSED") {
        actionButton.textContent = "休業日";
        actionButton.disabled = true;
      } else if (isAssignedToOther) {
        actionButton.textContent = "他ガイド担当";
        actionButton.disabled = true;
      } else if (isMine) {
        actionButton.textContent = "この枠から外れる";
        actionButton.addEventListener("click", () => updateOwnSlot(slot, false, "STOPPED", feedback));
      } else {
        actionButton.textContent = "この枠に入る";
        actionButton.addEventListener("click", () => updateOwnSlot(slot, true, statusSelect.value, feedback));
      }

      if (isMine) {
        const saveButton = document.createElement("button");
        saveButton.type = "button";
        saveButton.className = "btn btn-secondary";
        saveButton.textContent = "状態だけ更新";
        saveButton.addEventListener("click", () => updateOwnSlot(slot, true, statusSelect.value, feedback));
        actions.append(statusSelect, saveButton, actionButton);
      } else {
        actions.append(statusSelect, actionButton);
      }

      feedback.className = "admin-empty slot-editor-feedback";
      feedback.textContent = slot.effectiveStatus === "CLOSED"
        ? "holiday_control により休業です。"
        : isAssignedToOther
          ? "この枠は別のガイドが担当しています。"
          : isMine
            ? "必要なら状態だけ更新できます。"
            : "担当する場合はこの枠に入るを押してください。";

      field.append(label, meta, actions, feedback);
      editorList.append(field);
    });
  }

  async function loadMonth() {
    normalizeSelectedDay();
    syncLocation();

    if (!state.loginId) {
      state.monthData = null;
      rebuildCalendar();
      renderCalendar();
      renderSelectedDayEditor();
      setPageFeedback("ガイドを選択すると、本人用スケジュールを表示します。");
      return;
    }

    setPageFeedback("月間スケジュールを読み込んでいます。");

    try {
      const response = await fetch(`/api/guide/slots?loginId=${encodeURIComponent(state.loginId)}&year=${state.year}&month=${state.month}`);
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "スケジュール取得に失敗しました。");
      }

      state.monthData = await response.json();
      state.guideProfile = state.monthData.guide || state.guideProfile;
      updateProfile();
      populateGuideSelector();
      rebuildCalendar();
      renderCalendar();
      renderSelectedDayEditor();
      setPageFeedback(`${formatMonthText()} のスケジュールを読み込みました。`);
    } catch (error) {
      state.monthData = null;
      rebuildCalendar();
      renderSelectedDayEditor();
      setPageFeedback(error.message || "スケジュール取得に失敗しました。", true);
    }
  }

  function switchGuide() {
    const nextLoginId = guideSelector?.value?.trim() || "";
    state.loginId = nextLoginId;
    state.selectedDay = 1;
    loadCurrentGuide()
      .catch((error) => {
        state.loginId = "";
        state.guideProfile = null;
        updateProfile();
        setPageFeedback(error.message || "ガイド情報の取得に失敗しました。", true);
      })
      .finally(() => {
        loadMonth();
      });
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

  reloadButton?.addEventListener("click", () => loadMonth());
  guideSwitchButton?.addEventListener("click", switchGuide);
  guideSelector?.addEventListener("change", () => {
    if (guideSwitchButton) {
      guideSwitchButton.disabled = !guideSelector.value;
    }
  });

  loadGuideList()
    .then(() => loadCurrentGuide())
    .catch((error) => {
      setPageFeedback(error.message || "ガイド一覧の取得に失敗しました。", true);
    })
    .finally(() => {
      loadMonth();
    });
})();
