document.addEventListener("DOMContentLoaded", () => {
  const calendarCards = document.querySelectorAll(".calendar-card");
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
  const isJapanesePage = document.body.classList.contains("lang-ja");
  const unitPrice = 12000;
  const taxRate = 0.1;

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

  const getActiveCalendar = () => {
    if (guideJapaneseRadio?.checked) {
      return document.querySelector(".calendar-japanese");
    }

    return document.querySelector(".calendar-english");
  };

  const getSelectedDay = () => {
    const activeCalendar = getActiveCalendar();
    return activeCalendar?.querySelector(".day.selected:not(.muted)") ?? null;
  };

  const getFormattedDate = () => {
    const selectedDay = getSelectedDay();
    const day = selectedDay?.textContent?.trim() ?? "12";

    if (isJapanesePage) {
      return `2026年5月${day}日`;
    }

    return `May ${day}, 2026`;
  };

  const updateConfirmationSummary = () => {
    const guestCount = getGuestCount();
    const isEnglishGuide = guideEnglishRadio?.checked ?? true;

    if (confirmationDate) {
      confirmationDate.textContent = getFormattedDate();
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
        ? (isEnglishGuide ? "英語" : "日本語")
        : (isEnglishGuide ? "English" : "Japanese");
    }
  };

  const updatePriceSummary = () => {
    if (!priceItemLabel || !priceItemAmount || !priceTotalAmount) {
      return;
    }

    const guestCount = getGuestCount();
    const isEnglishGuide = guideEnglishRadio?.checked ?? true;
    const subtotal = guestCount * unitPrice;
    const consumptionTax = Math.round(subtotal * taxRate);
    const total = subtotal + consumptionTax;

    if (isJapanesePage) {
      priceItemLabel.textContent = isEnglishGuide
        ? `ワークショップ（英語）× ${guestCount}名`
        : `ワークショップ（日本語）× ${guestCount}名`;
    } else {
      priceItemLabel.textContent = isEnglishGuide
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

  calendarCards.forEach((card) => {
    syncPressedState(card);

    card.addEventListener("click", (event) => {
      const day = event.target.closest(".day");
      if (!day || day.classList.contains("muted")) {
        return;
      }

      card.querySelectorAll(".day.selected").forEach((selectedDay) => {
        selectedDay.classList.remove("selected");
      });

      day.classList.add("selected");
      syncPressedState(card);
      updateConfirmationSummary();
    });
  });

  guestCountSelect?.addEventListener("change", updatePriceSummary);
  timeSlotSelect?.addEventListener("change", updateConfirmationSummary);
  guideEnglishRadio?.addEventListener("change", updatePriceSummary);
  guideJapaneseRadio?.addEventListener("change", updatePriceSummary);
  updatePriceSummary();
});
