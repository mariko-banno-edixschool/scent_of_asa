document.addEventListener("DOMContentLoaded", () => {
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
  const isJapanesePage = document.body.classList.contains("lang-ja");
  const unitPrice = 12000;
  const taxRate = 0.1;
  const monthCursor = new Date(2026, 4, 1);
  const calendarCards = {
    english: document.querySelector(".calendar-english"),
    japanese: document.querySelector(".calendar-japanese"),
  };
  const selectedDates = {
    english: new Date(2026, 4, 12),
    japanese: new Date(2026, 4, 7),
  };
  const soldOutDays = {
    english: [2, 3, 9, 30, 31],
    japanese: [2, 3, 9, 30, 31],
  };

  const syncPressedState = (card) => {
    card.querySelectorAll(".day").forEach((day) => {
      day.setAttribute("aria-pressed", day.classList.contains("selected") ? "true" : "false");
    });
  };

  const formatPrice = (amount) => {
    if (isJapanesePage) {
      return `${amount.toLocaleString("ja-JP")}円`;
    }

    return `JPY ${amount.toLocaleString("en-US")}`;
  };

  const getGuestCount = () => {
    if (!guestCountSelect) {
      return 1;
    }

    const match = guestCountSelect.value.match(/\d+/);
    return match ? Number(match[0]) : 1;
  };

  const getActiveGuide = () => (guideJapaneseRadio?.checked ? "japanese" : "english");

  const formatSummaryDate = (date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const day = date.getDate();

    if (isJapanesePage) {
      return `${year}年${month + 1}月${day}日`;
    }

    return new Intl.DateTimeFormat("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }).format(date);
  };

  const updateConfirmationSummary = () => {
    const guestCount = getGuestCount();
    const activeGuide = getActiveGuide();

    if (confirmationDate) {
      confirmationDate.textContent = formatSummaryDate(selectedDates[activeGuide]);
    }

    if (confirmationTime && timeSlotSelect) {
      confirmationTime.textContent = timeSlotSelect.value;
    }

    if (confirmationGuests) {
      confirmationGuests.textContent = isJapanesePage
        ? `${guestCount}名`
        : `${guestCount} ${guestCount === 1 ? "Guest" : "Guests"}`;
    }

    if (confirmationLanguage) {
      confirmationLanguage.textContent = isJapanesePage
        ? (activeGuide === "english" ? "英語" : "日本語")
        : (activeGuide === "english" ? "English" : "Japanese");
    }
  };

  const updatePriceSummary = () => {
    if (!priceItemLabel || !priceItemAmount || !priceTotalAmount) {
      return;
    }

    const guestCount = getGuestCount();
    const activeGuide = getActiveGuide();
    const subtotal = guestCount * unitPrice;
    const consumptionTax = Math.round(subtotal * taxRate);
    const total = subtotal + consumptionTax;

    if (isJapanesePage) {
      priceItemLabel.textContent = activeGuide === "english"
        ? `ワークショップ（英語）× ${guestCount}名`
        : `ワークショップ（日本語）× ${guestCount}名`;
    } else {
      priceItemLabel.textContent = activeGuide === "english"
        ? `Workshop (EN) * ${guestCount} ${guestCount === 1 ? "guest" : "guests"}`
        : `Workshop (JP) * ${guestCount} ${guestCount === 1 ? "guest" : "guests"}`;
    }

    priceItemAmount.textContent = formatPrice(subtotal);

    if (serviceFeeAmount) {
      serviceFeeAmount.textContent = formatPrice(consumptionTax);
    }

    priceTotalAmount.textContent = formatPrice(total);
    updateConfirmationSummary();
  };

  const updateMonthLabel = () => {
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
  };

  const renderCalendar = (guideKey) => {
    const card = calendarCards[guideKey];
    if (!card) {
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
    const soldOutSet = new Set(soldOutDays[guideKey]);

    grid.innerHTML = "";

    for (let i = firstDay - 1; i >= 0; i -= 1) {
      const button = document.createElement("button");
      button.className = "day muted";
      button.type = "button";
      button.textContent = String(prevMonthDays - i);
      grid.appendChild(button);
    }

    for (let day = 1; day <= daysInMonth; day += 1) {
      const button = document.createElement("button");
      button.className = "day";
      button.type = "button";
      button.textContent = String(day);
      button.dataset.guide = guideKey;
      button.dataset.year = String(year);
      button.dataset.month = String(month);
      button.dataset.day = String(day);

      if (soldOutSet.has(day)) {
        button.classList.add("soldout");
      }

      if (
        selectedDate.getFullYear() === year &&
        selectedDate.getMonth() === month &&
        selectedDate.getDate() === day
      ) {
        button.classList.add("selected");
      }

      grid.appendChild(button);
    }

    const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
    const trailingDays = totalCells - (firstDay + daysInMonth);

    for (let day = 1; day <= trailingDays; day += 1) {
      const button = document.createElement("button");
      button.className = "day muted";
      button.type = "button";
      button.textContent = String(day);
      grid.appendChild(button);
    }

    syncPressedState(card);
  };

  const renderCalendars = () => {
    updateMonthLabel();
    renderCalendar("english");
    renderCalendar("japanese");
  };

  const moveMonth = (delta) => {
    monthCursor.setMonth(monthCursor.getMonth() + delta);
    renderCalendars();
  };

  Object.entries(calendarCards).forEach(([guideKey, card]) => {
    if (!card) {
      return;
    }

    card.addEventListener("click", (event) => {
      const day = event.target.closest(".day");
      if (!day || day.classList.contains("muted") || day.classList.contains("soldout")) {
        return;
      }

      const year = Number(day.dataset.year);
      const month = Number(day.dataset.month);
      const date = Number(day.dataset.day);
      selectedDates[guideKey] = new Date(year, month, date);
      renderCalendar(guideKey);
      updateConfirmationSummary();
    });
  });

  prevButton?.addEventListener("click", () => moveMonth(-1));
  nextButton?.addEventListener("click", () => moveMonth(1));
  guestCountSelect?.addEventListener("change", updatePriceSummary);
  timeSlotSelect?.addEventListener("change", updateConfirmationSummary);
  guideEnglishRadio?.addEventListener("change", updatePriceSummary);
  guideJapaneseRadio?.addEventListener("change", updatePriceSummary);

  renderCalendars();
  updatePriceSummary();
});
