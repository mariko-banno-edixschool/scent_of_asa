document.addEventListener("DOMContentLoaded", () => {
  const confirmationStorageKey = "scent_of_asa_public_reservation";
  const isJapanesePage = document.body.classList.contains("lang-ja");
  const reservation = readReservation();

  renderConfirmation(reservation);

  function readReservation() {
    const raw = window.sessionStorage.getItem(confirmationStorageKey);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw);
    } catch (_error) {
      return null;
    }
  }

  function formatDate(dateString) {
    if (!dateString) {
      return isJapanesePage ? "未設定" : "Not available";
    }

    const date = new Date(`${dateString}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
      return dateString;
    }

    if (isJapanesePage) {
      return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
    }

    return new Intl.DateTimeFormat("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }).format(date);
  }

  function formatCompletedAt(dateTimeString) {
    if (!dateTimeString) {
      return isJapanesePage ? "未設定" : "Not available";
    }

    const date = new Date(dateTimeString);
    if (Number.isNaN(date.getTime())) {
      return dateTimeString;
    }

    if (isJapanesePage) {
      return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
    }

    return new Intl.DateTimeFormat("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    }).format(date);
  }

  function formatGuestCount(guestCount) {
    const count = Number(guestCount || 0);
    if (isJapanesePage) {
      return `${count}名`;
    }
    return `${count} ${count === 1 ? "Guest" : "Guests"}`;
  }

  function formatLanguage(language) {
    return language === "ja"
      ? (isJapanesePage ? "日本語" : "Japanese")
      : (isJapanesePage ? "英語" : "English");
  }

  function formatTimeSlot(timeSlot) {
    if (timeSlot === "11:00") return "11:00 - 12:30";
    if (timeSlot === "13:00") return "13:00 - 14:30";
    if (timeSlot === "15:30") return "15:30 - 17:00";
    return timeSlot || (isJapanesePage ? "未設定" : "Not available");
  }

  function formatPrice(amount) {
    const value = Number(amount || 0);
    if (isJapanesePage) {
      return `${value.toLocaleString("ja-JP")}円`;
    }
    return `JPY ${value.toLocaleString("en-US")}`;
  }

  function setText(id, value) {
    const element = document.querySelector(`#${id}`);
    if (element) {
      element.textContent = value;
    }
  }

  function setTextByIndex(selector, index, value) {
    const element = document.querySelectorAll(selector)[index];
    if (element) {
      element.textContent = value;
    }
  }

  function renderConfirmation(data) {
    const emptyMessage = document.querySelector("#confirmation-empty-message");
    if (!data) {
      if (emptyMessage) {
        emptyMessage.textContent = isJapanesePage
          ? "予約情報が見つかりません。予約ページに戻って、もう一度お手続きください。"
          : "No reservation details were found. Please return to the booking page and complete your reservation again.";
      }
      const fallback = isJapanesePage ? "未設定" : "Not available";
      [
        "confirmation-reservation-code",
        "confirmation-completed-date",
        "confirmation-detail-time",
        "confirmation-detail-guests",
        "confirmation-detail-language",
        "confirmation-detail-name",
        "confirmation-detail-email",
        "confirmation-summary-date",
        "confirmation-summary-time",
        "confirmation-summary-guests",
        "confirmation-summary-language",
      ].forEach((id) => setText(id, fallback));
      return;
    }

    if (emptyMessage) {
      emptyMessage.textContent = isJapanesePage
        ? "ご登録内容を以下に表示しています。当日は開始5〜10分前を目安にお越しください。"
        : "Your reservation details are shown below. Please arrive about 5 to 10 minutes before your workshop begins.";
    }

    const guestCountLabel = formatGuestCount(data.guestCount);
    const languageLabel = formatLanguage(data.guideLanguage);
    const visitDateLabel = formatDate(data.reservationDate);
    const completedDateLabel = formatCompletedAt(data.completedAt);
    const timeLabel = formatTimeSlot(data.timeSlot);
    const subtotal = Number(data.guestCount || 0) * 12000;
    const tax = Math.round(subtotal * 0.1);
    const total = subtotal + tax;
    const priceLabel = data.guideLanguage === "ja"
      ? (isJapanesePage
        ? `ワークショップ（日本語）× ${guestCountLabel}`
        : `Workshop (JP) * ${Number(data.guestCount || 0)} ${Number(data.guestCount || 0) === 1 ? "guest" : "guests"}`)
      : (isJapanesePage
        ? `ワークショップ（英語）× ${guestCountLabel}`
        : `Workshop (EN) * ${Number(data.guestCount || 0)} ${Number(data.guestCount || 0) === 1 ? "guest" : "guests"}`);

    setText("confirmation-reservation-code", data.reservationCode || data.reservationId || "-");
    setText("confirmation-completed-date", completedDateLabel);
    setText("confirmation-detail-time", timeLabel);
    setText("confirmation-detail-guests", guestCountLabel);
    setText("confirmation-detail-language", languageLabel);
    setText("confirmation-detail-name", data.customerName || (isJapanesePage ? "未設定" : "Not available"));
    setText("confirmation-detail-email", data.customerEmail || (isJapanesePage ? "未設定" : "Not available"));
    setText("confirmation-summary-date", visitDateLabel);
    setText("confirmation-summary-time", timeLabel);
    setText("confirmation-summary-guests", guestCountLabel);
    setText("confirmation-summary-language", languageLabel);
    setText("confirmation-price-label", priceLabel);
    setText("confirmation-price-subtotal", formatPrice(subtotal));
    setText("confirmation-price-tax", formatPrice(tax));
    setText("confirmation-price-total", formatPrice(total));

    setTextByIndex(".detail-row .detail-value", 0, data.reservationCode || data.reservationId || "-");
    setTextByIndex(".detail-row .detail-value", 2, completedDateLabel);
    setTextByIndex(".detail-row .detail-value", 3, timeLabel);
    setTextByIndex(".detail-row .detail-value", 4, guestCountLabel);
    setTextByIndex(".detail-row .detail-value", 5, languageLabel);
    setTextByIndex(".detail-row .detail-value", 6, data.customerName || (isJapanesePage ? "未設定" : "Not available"));
    setTextByIndex(".detail-row .detail-value", 7, data.customerEmail || (isJapanesePage ? "未設定" : "Not available"));
    setTextByIndex(".confirmation-row strong", 0, visitDateLabel);
    setTextByIndex(".confirmation-row strong", 1, timeLabel);
    setTextByIndex(".confirmation-row strong", 2, guestCountLabel);
    setTextByIndex(".confirmation-row strong", 3, languageLabel);
    setTextByIndex(".price-row span", 0, priceLabel);
    setTextByIndex(".price-row strong", 0, formatPrice(subtotal));
    setTextByIndex(".price-row strong", 1, formatPrice(tax));
    setTextByIndex(".price-row strong", 2, formatPrice(total));
  }
});
