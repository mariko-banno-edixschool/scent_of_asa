document.addEventListener("DOMContentLoaded", () => {
  const calendarCards = document.querySelectorAll(".calendar-card");
  const guestCountSelect = document.querySelector("#guest-count");
  const guideEnglishRadio = document.querySelector("#guide-english");
  const guideJapaneseRadio = document.querySelector("#guide-japanese");
  const priceItemLabel = document.querySelector("#price-item-label");
  const priceItemAmount = document.querySelector("#price-item-amount");
  const serviceFeeAmount = document.querySelector("#service-fee-amount");
  const priceTotalAmount = document.querySelector("#price-total-amount");
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
    });
  });

  guestCountSelect?.addEventListener("change", updatePriceSummary);
  guideEnglishRadio?.addEventListener("change", updatePriceSummary);
  guideJapaneseRadio?.addEventListener("change", updatePriceSummary);
  updatePriceSummary();
});
