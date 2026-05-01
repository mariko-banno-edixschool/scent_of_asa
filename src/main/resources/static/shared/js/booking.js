document.addEventListener("DOMContentLoaded", () => {
  const confirmationStorageKey = "scent_of_asa_public_reservation";
  const guestCountSelect = document.querySelector("#guest-count");
  const timeSlotSelect = document.querySelector("#time-slot");
  const guideEnglishRadio = document.querySelector("#guide-english");
  const guideJapaneseRadio = document.querySelector("#guide-japanese");
  const priceItemLabel = document.querySelector("#price-item-label");
  const priceItemAmount = document.querySelector("#price-item-amount");
  const serviceFeeAmount = document.querySelector("#service-fee-amount");
  const priceTotalAmount = document.querySelector("#price-total-amount");
  const confirmationDate = document.querySelector("#confirmation-date");
  const confirmationTime = document.querySelector("#confirmation-time");
  const confirmationGuests = document.querySelector("#confirmation-guests");
  const confirmationLanguage = document.querySelector("#confirmation-language");
  const monthLabel = document.querySelector(".calendar-nav span");
  const prevButton = document.querySelector(".calendar-nav button:first-child");
  const nextButton = document.querySelector(".calendar-nav button:last-child");
  const submitButton = document.querySelector("#booking-submit") || document.querySelector(".price-card .btn-primary");
  const isJapanesePage = document.body.classList.contains("lang-ja");
  const unitPrice = 12000;
  const taxRate = 0.1;
  const today = new Date();
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  const currentMonthStart = new Date(today.getFullYear(), today.getMonth(), 1);
  const monthCursor = new Date(currentMonthStart);

  const calendarCards = {
    english: document.querySelector(".calendar-english"),
    japanese: document.querySelector(".calendar-japanese"),
  };

  if (!guestCountSelect || !timeSlotSelect || !calendarCards.english || !calendarCards.japanese) {
    return;
  }

  const contactForm = document.querySelector("#contact .booking-form");
  const contactTextInputs = contactForm ? [...contactForm.querySelectorAll('input[type="text"]')] : [];
  const firstNameInput = document.querySelector("#customer-first-name") || contactTextInputs[0] || null;
  const lastNameInput = document.querySelector("#customer-last-name") || contactTextInputs[1] || null;
  const emailInput = document.querySelector("#customer-email") || (contactForm ? contactForm.querySelector('input[type="email"]') : null);
  const phoneInput = document.querySelector("#customer-phone") || (contactForm ? contactForm.querySelector('input[type="tel"]') : null);
  const notesInput = document.querySelector("#customer-notes") || (contactForm ? contactForm.querySelector("textarea") : null);

  const slotDisplayLabels = {
    "11:00": "11:00 - 12:30",
    "13:00": "13:00 - 14:30",
    "15:30": "15:30 - 17:00",
  };

  const availabilityByLanguage = {
    english: null,
    japanese: null,
  };
  const selectedDates = {
    english: null,
    japanese: null,
  };
  const selectedSlots = {
    english: null,
    japanese: null,
  };
  let isSubmitting = false;
  const submitButtonDefaultLabel = submitButton ? submitButton.textContent : "";
  const submitStatus = createSubmitStatus();

  function createSubmitStatus() {
    if (!submitButton || !submitButton.parentElement) {
      return null;
    }

    const status = document.createElement("p");
    status.className = "mini-note";
    status.style.marginTop = "10px";
    status.style.minHeight = "1.4em";
    status.style.color = "var(--muted)";
    submitButton.insertAdjacentElement("afterend", status);
    return status;
  }

  function isBeforeCurrentMonth() {
    return monthCursor.getFullYear() < currentMonthStart.getFullYear()
      || (monthCursor.getFullYear() === currentMonthStart.getFullYear() && monthCursor.getMonth() < currentMonthStart.getMonth());
  }

  function isPastDate(isoDate) {
    return new Date(`${isoDate}T00:00:00`).getTime() < todayStart.getTime();
  }

  function isBookingClosedDay(dayData) {
    return !!dayData?.bookingClosed;
  }

  function toLocalIsoDate(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  }

  function normalizeOptional(value) {
    return value && value.trim() ? value.trim() : null;
  }

  function buildCustomerName() {
    return [firstNameInput?.value?.trim(), lastNameInput?.value?.trim()]
      .filter(Boolean)
      .join(" ")
      .trim();
  }

  function buildReservationPayload() {
    const activeGuide = getActiveGuide();
    const selectedDate = selectedDates[activeGuide];
    return {
      reservationDate: selectedDate ? toLocalIsoDate(selectedDate) : null,
      timeSlot: selectedSlots[activeGuide] || null,
      guideLanguage: getGuideApiLanguage(activeGuide),
      guestCount: getGuestCount(),
      customerName: buildCustomerName(),
      customerEmail: emailInput?.value?.trim() || "",
      customerPhone: normalizeOptional(phoneInput?.value || ""),
      notes: normalizeOptional(notesInput?.value || ""),
    };
  }

  function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  function getConfirmationPageUrl() {
    return isJapanesePage ? "confirmation-ja.html" : "confirmation.html";
  }

  function setSubmitState(submitting, message = "", isError = false) {
    isSubmitting = submitting;
    if (submitButton) {
      submitButton.setAttribute("aria-disabled", submitting ? "true" : "false");
      submitButton.textContent = submitting
        ? (isJapanesePage ? "送信中..." : "Submitting...")
        : submitButtonDefaultLabel;
      submitButton.style.pointerEvents = submitting ? "none" : "";
      submitButton.style.opacity = submitting ? "0.72" : "";
    }
    if (submitStatus) {
      submitStatus.textContent = message;
      submitStatus.style.color = isError ? "#9f403d" : "var(--muted)";
    }
  }

  function getConflictMessage(message) {
    if (message === "The selected slot is no longer available.") {
      return isJapanesePage
        ? "選択した枠は満席、または他のお客様の予約で埋まりました。別の日時を選んでください。"
        : "That slot just became unavailable. Please choose another date or time.";
    }
    if (message === "Reservations for the selected date have already closed.") {
      return isJapanesePage
        ? "当日と翌日の予約受付は終了しました。別の日付を選んでください。"
        : "Reservations for the selected date have already closed. Please choose a later date.";
    }
    return message;
  }

  function getNetworkRetryMessage() {
    return isJapanesePage
      ? "通信に失敗しました。時間をおいて再試行してください。"
      : "We couldn't complete the request. Please wait a moment and try again.";
  }

  function getValidationMessage(payload) {
    if (!payload.reservationDate) {
      return isJapanesePage ? "予約日を選択してください。" : "Please select a reservation date.";
    }
    if (!payload.timeSlot || timeSlotSelect.disabled) {
      return isJapanesePage ? "時間帯を選択してください。" : "Please select a time slot.";
    }
    if (!payload.guestCount || guestCountSelect.disabled) {
      return isJapanesePage ? "人数を選択してください。" : "Please select the number of guests.";
    }
    if (!firstNameInput?.value?.trim()) {
      return isJapanesePage ? "名を入力してください。" : "Please enter the first name.";
    }
    if (!lastNameInput?.value?.trim()) {
      return isJapanesePage ? "姓を入力してください。" : "Please enter the last name.";
    }
    if (!payload.customerEmail) {
      return isJapanesePage ? "メールアドレスを入力してください。" : "Please enter an email address.";
    }
    if (!isValidEmail(payload.customerEmail)) {
      return isJapanesePage ? "メールアドレスの形式を確認してください。" : "Please enter a valid email address.";
    }
    if (!payload.customerPhone) {
      return isJapanesePage ? "電話番号を入力してください。" : "Please enter a phone number.";
    }
    return null;
  }

  async function submitReservation(event) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const payload = buildReservationPayload();
    const validationMessage = getValidationMessage(payload);

    if (validationMessage) {
      window.alert(validationMessage);
      return;
    }

    try {
      setSubmitState(true, isJapanesePage ? "予約内容を送信しています..." : "Submitting your reservation...");
      const response = await fetch("/api/public/reservations", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const message = response.status === 409
          ? getConflictMessage(error.message || "The selected slot is no longer available.")
          : (error.message || (isJapanesePage ? "予約の送信に失敗しました。" : "Failed to submit the reservation."));
        throw new Error(message);
      }

      const reservation = await response.json();
      const successMessage = isJapanesePage
        ? `予約を受け付けました。予約番号: ${reservation.reservationCode || reservation.reservationId}`
        : `Your reservation has been received. Reservation code: ${reservation.reservationCode || reservation.reservationId}`;
      const confirmationPayload = {
        reservationId: reservation.reservationId ?? reservation.id ?? null,
        reservationCode: reservation.reservationCode || null,
        reservationDate: payload.reservationDate,
        timeSlot: payload.timeSlot,
        guideLanguage: payload.guideLanguage,
        guestCount: payload.guestCount,
        customerName: payload.customerName,
        customerEmail: payload.customerEmail,
        customerPhone: payload.customerPhone,
        notes: payload.notes,
        reservationStatus: reservation.status || reservation.reservationStatus || null,
        completedAt: new Date().toISOString(),
      };
      window.sessionStorage.setItem(confirmationStorageKey, JSON.stringify(confirmationPayload));
      setSubmitState(true, isJapanesePage ? "予約が完了しました。確認画面へ移動します..." : "Reservation complete. Redirecting to the confirmation page...");
      window.location.href = getConfirmationPageUrl();
    } catch (error) {
      const message = error instanceof TypeError
        ? getNetworkRetryMessage()
        : getConflictMessage(error.message || (isJapanesePage ? "予約の送信に失敗しました。" : "Failed to submit the reservation."));
      setSubmitState(false, message, true);
      window.alert(message);
    }
  }

  function formatPrice(amount) {
    if (isJapanesePage) {
      return `${amount.toLocaleString("ja-JP")}円`;
    }
    return `JPY ${amount.toLocaleString("en-US")}`;
  }

  function getGuestCount() {
    const match = guestCountSelect.value.match(/\d+/);
    return match ? Number(match[0]) : 1;
  }

  function getActiveGuide() {
    return guideJapaneseRadio?.checked ? "japanese" : "english";
  }

  function getGuideApiLanguage(guideKey) {
    return guideKey === "english" ? "en" : "ja";
  }

  function getGuideSummaryLabel(guideKey) {
    if (isJapanesePage) {
      return guideKey === "english" ? "英語" : "日本語";
    }
    return guideKey === "english" ? "English" : "Japanese";
  }

  function getGuidePriceLabel(guideKey, guestCount) {
    if (isJapanesePage) {
      return guideKey === "english"
        ? `ワークショップ（英語）× ${guestCount}名`
        : `ワークショップ（日本語）× ${guestCount}名`;
    }
    return guideKey === "english"
      ? `Workshop (EN) * ${guestCount} ${guestCount === 1 ? "guest" : "guests"}`
      : `Workshop (JP) * ${guestCount} ${guestCount === 1 ? "guest" : "guests"}`;
  }

  function getSummaryGuestLabel(guestCount) {
    return isJapanesePage
      ? `${guestCount}名`
      : `${guestCount} ${guestCount === 1 ? "Guest" : "Guests"}`;
  }

  function getTimeDisplayLabel(timeSlot) {
    return slotDisplayLabels[timeSlot] || timeSlot;
  }

  function getSlotOptionLabel(slot) {
    const timeLabel = getTimeDisplayLabel(slot.timeSlot);
    if (slot.status === "LIMITED") {
      return isJapanesePage
        ? `${timeLabel}（残少）`
        : `${timeLabel} (Limited)`;
    }
    return timeLabel;
  }

  function formatSummaryDate(date) {
    if (!date) {
      return isJapanesePage ? "未選択" : "Not selected";
    }

    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();

    if (isJapanesePage) {
      return `${year}年${month}月${day}日`;
    }

    return new Intl.DateTimeFormat("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }).format(date);
  }

  function getSelectedDayData(guideKey) {
    const selectedDate = selectedDates[guideKey];
    const availability = availabilityByLanguage[guideKey];
    if (!selectedDate || !availability) {
      return null;
    }

    const isoDate = toLocalIsoDate(selectedDate);
    return availability.days.find((day) => day.date === isoDate) || null;
  }

  function getReservableSlots(guideKey) {
    const dayData = getSelectedDayData(guideKey);
    if (!dayData) {
      return [];
    }

    return dayData.slots.filter((slot) => slot.status === "OPEN" || slot.status === "LIMITED");
  }

  function syncSelectedDateForMonth(guideKey) {
    const availability = availabilityByLanguage[guideKey];
    if (!availability) {
      selectedDates[guideKey] = null;
      selectedSlots[guideKey] = null;
      return;
    }

    const currentSelection = selectedDates[guideKey];
    if (currentSelection) {
      const isoDate = toLocalIsoDate(currentSelection);
      const existingDay = availability.days.find((day) => day.date === isoDate);
      if (existingDay && !isPastDate(existingDay.date) && existingDay.slots.some((slot) => slot.available)) {
        return;
      }
    }

    const firstAvailableDay = availability.days.find((day) => !isPastDate(day.date) && day.slots.some((slot) => slot.available));
    selectedDates[guideKey] = firstAvailableDay ? new Date(`${firstAvailableDay.date}T00:00:00`) : null;
    selectedSlots[guideKey] = null;
  }

  function syncSelectedSlot(guideKey) {
    const reservableSlots = getReservableSlots(guideKey);
    if (reservableSlots.length === 0) {
      selectedSlots[guideKey] = null;
      return;
    }

    const currentSelection = selectedSlots[guideKey];
    const matchingSlot = reservableSlots.find((slot) => slot.timeSlot === currentSelection);
    selectedSlots[guideKey] = matchingSlot ? matchingSlot.timeSlot : reservableSlots[0].timeSlot;
  }

  function updateGuestCountOptions() {
    const activeGuide = getActiveGuide();
    const reservableSlots = getReservableSlots(activeGuide);
    const selectedSlot = reservableSlots.find((slot) => slot.timeSlot === selectedSlots[activeGuide]);

    guestCountSelect.innerHTML = "";

    if (!selectedSlot) {
      const option = document.createElement("option");
      option.textContent = isJapanesePage ? "選択できる人数がありません" : "No guest count available";
      guestCountSelect.append(option);
      guestCountSelect.disabled = true;
      return;
    }

    guestCountSelect.disabled = false;
    for (let guestCount = 1; guestCount <= selectedSlot.remainingCapacity; guestCount += 1) {
      const option = document.createElement("option");
      option.value = isJapanesePage ? `${guestCount}名` : `${guestCount} ${guestCount === 1 ? "Guest" : "Guests"}`;
      option.textContent = option.value;
      guestCountSelect.append(option);
    }
  }

  function updateTimeSlotOptions() {
    const activeGuide = getActiveGuide();
    const reservableSlots = getReservableSlots(activeGuide);

    timeSlotSelect.innerHTML = "";

    if (reservableSlots.length === 0) {
      const option = document.createElement("option");
      option.textContent = isJapanesePage ? "選択できる時間帯がありません" : "No slots available";
      timeSlotSelect.append(option);
      timeSlotSelect.disabled = true;
      updateGuestCountOptions();
      updatePriceSummary();
      return;
    }

    timeSlotSelect.disabled = false;
    syncSelectedSlot(activeGuide);

    reservableSlots.forEach((slot) => {
      const option = document.createElement("option");
      option.value = slot.timeSlot;
      option.textContent = getSlotOptionLabel(slot);
      option.selected = slot.timeSlot === selectedSlots[activeGuide];
      timeSlotSelect.append(option);
    });

    updateGuestCountOptions();
    updatePriceSummary();
  }

  function updateConfirmationSummary() {
    const activeGuide = getActiveGuide();
    const guestCount = getGuestCount();
    const selectedDate = selectedDates[activeGuide];

    if (confirmationDate) {
      confirmationDate.textContent = formatSummaryDate(selectedDate);
    }

    if (confirmationTime) {
      confirmationTime.textContent = timeSlotSelect.disabled ? (isJapanesePage ? "未選択" : "Not selected") : getTimeDisplayLabel(selectedSlots[activeGuide]);
    }

    if (confirmationGuests) {
      confirmationGuests.textContent = timeSlotSelect.disabled ? (isJapanesePage ? "未選択" : "Not selected") : getSummaryGuestLabel(guestCount);
    }

    if (confirmationLanguage) {
      confirmationLanguage.textContent = getGuideSummaryLabel(activeGuide);
    }
  }

  function updatePriceSummary() {
    const guestCount = timeSlotSelect.disabled ? 0 : getGuestCount();
    const activeGuide = getActiveGuide();
    const subtotal = guestCount * unitPrice;
    const consumptionTax = Math.round(subtotal * taxRate);
    const total = subtotal + consumptionTax;

    if (priceItemLabel) {
      priceItemLabel.textContent = getGuidePriceLabel(activeGuide, guestCount || 0);
    }
    if (priceItemAmount) {
      priceItemAmount.textContent = formatPrice(subtotal);
    }
    if (serviceFeeAmount) {
      serviceFeeAmount.textContent = formatPrice(consumptionTax);
    }
    if (priceTotalAmount) {
      priceTotalAmount.textContent = formatPrice(total);
    }

    updateConfirmationSummary();
  }

  function updateMonthLabel() {
    if (!monthLabel) {
      return;
    }

    if (isJapanesePage) {
      monthLabel.textContent = `${monthCursor.getFullYear()}年${monthCursor.getMonth() + 1}月`;
      return;
    }

    monthLabel.textContent = new Intl.DateTimeFormat("en-US", {
      year: "numeric",
      month: "long",
    }).format(monthCursor);
  }

  function updateMonthNavigation() {
    if (prevButton) {
      prevButton.disabled = isBeforeCurrentMonth()
        || (monthCursor.getFullYear() === currentMonthStart.getFullYear() && monthCursor.getMonth() === currentMonthStart.getMonth());
    }
  }

  function createMutedDayButton(text) {
    const button = document.createElement("button");
    button.className = "day muted";
    button.type = "button";
    button.textContent = String(text);
    button.disabled = true;
    return button;
  }

  function renderCalendar(guideKey) {
    const card = calendarCards[guideKey];
    const availability = availabilityByLanguage[guideKey];
    if (!card || !availability) {
      return;
    }

    const grid = card.querySelector(".calendar-grid");
    if (!grid) {
      return;
    }

    const year = monthCursor.getFullYear();
    const month = monthCursor.getMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const prevMonthDays = new Date(year, month, 0).getDate();
    const selectedDate = selectedDates[guideKey];

    grid.innerHTML = "";

    for (let i = firstDay - 1; i >= 0; i -= 1) {
      grid.append(createMutedDayButton(prevMonthDays - i));
    }

    for (let day = 1; day <= daysInMonth; day += 1) {
      const button = document.createElement("button");
      const isoDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
      const dayData = availability.days.find((entry) => entry.date === isoDate);
      const hasAvailableSlot = !!dayData && !isPastDate(isoDate) && dayData.slots.some((slot) => slot.available);
      const isPast = isPastDate(isoDate);
      const bookingClosed = isBookingClosedDay(dayData);

      button.className = "day";
      button.type = "button";
      button.textContent = String(day);
      button.dataset.guide = guideKey;
      button.dataset.date = isoDate;

      if (isPast) {
        button.classList.add("muted");
        button.disabled = true;
      } else if (bookingClosed || !hasAvailableSlot) {
        button.classList.add("soldout");
      }

      if (selectedDate && toLocalIsoDate(selectedDate) === isoDate) {
        button.classList.add("selected");
      }

      grid.append(button);
    }

    const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
    const trailingDays = totalCells - (firstDay + daysInMonth);
    for (let day = 1; day <= trailingDays; day += 1) {
      grid.append(createMutedDayButton(day));
    }
  }

  function renderCalendars() {
    updateMonthLabel();
    updateMonthNavigation();
    renderCalendar("english");
    renderCalendar("japanese");
  }

  async function fetchAvailability(guideKey) {
    const response = await fetch(`/api/public/availability?year=${monthCursor.getFullYear()}&month=${monthCursor.getMonth() + 1}&language=${getGuideApiLanguage(guideKey)}`);
    if (!response.ok) {
      throw new Error("Failed to load availability.");
    }
    availabilityByLanguage[guideKey] = await response.json();
    syncSelectedDateForMonth(guideKey);
    syncSelectedSlot(guideKey);
  }

  async function loadMonth() {
    await Promise.all([fetchAvailability("english"), fetchAvailability("japanese")]);
    renderCalendars();
    updateTimeSlotOptions();
  }

  function moveMonth(delta) {
    monthCursor.setMonth(monthCursor.getMonth() + delta);
    if (isBeforeCurrentMonth()) {
      monthCursor.setTime(currentMonthStart.getTime());
    }
    loadMonth().catch(() => {
      updateMonthLabel();
      updateMonthNavigation();
    });
  }

  Object.entries(calendarCards).forEach(([guideKey, card]) => {
    card.addEventListener("click", (event) => {
      const day = event.target.closest(".day");
      if (!day || day.classList.contains("muted") || day.classList.contains("soldout")) {
        return;
      }

      selectedDates[guideKey] = new Date(`${day.dataset.date}T00:00:00`);
      selectedSlots[guideKey] = null;
      syncSelectedSlot(guideKey);
      renderCalendar(guideKey);
      if (getActiveGuide() === guideKey) {
        updateTimeSlotOptions();
      }
    });
  });

  prevButton?.addEventListener("click", () => moveMonth(-1));
  nextButton?.addEventListener("click", () => moveMonth(1));

  timeSlotSelect.addEventListener("change", () => {
    selectedSlots[getActiveGuide()] = timeSlotSelect.value;
    updateGuestCountOptions();
    updatePriceSummary();
  });

  guestCountSelect.addEventListener("change", updatePriceSummary);
  guideEnglishRadio?.addEventListener("change", () => {
    updateTimeSlotOptions();
    renderCalendars();
  });
  guideJapaneseRadio?.addEventListener("change", () => {
    updateTimeSlotOptions();
    renderCalendars();
  });

  loadMonth().catch(() => {
    updateMonthLabel();
    updatePriceSummary();
  });

  submitButton?.addEventListener("click", submitReservation);
});
