(() => {
  const feedback = document.querySelector("#reservation-status-feedback");
  const dateInput = document.querySelector("#reservation-filter-date");
  const customerNameInput = document.querySelector("#reservation-filter-name");
  const languageSelect = document.querySelector("#reservation-filter-language");
  const searchButton = document.querySelector("#reservation-filter-search");
  const exportButton = document.querySelector("#reservation-export");
  const countBadge = document.querySelector("#reservation-total-count");
  const tableBody = document.querySelector("#reservation-table-body");

  if (!tableBody) {
    return;
  }

  const labels = {
    PENDING: "調整中",
    CONFIRMED: "予約確定",
    PAID: "支払い済み",
    CHECKED_IN: "チェックイン済み",
    CANCELLED: "キャンセル",
    NO_SHOW: "無断欠席",
  };

  const classes = {
    PENDING: "is-waiting",
    CONFIRMED: "",
    PAID: "is-paid",
    CHECKED_IN: "is-done",
    CANCELLED: "is-alert",
    NO_SHOW: "is-stop",
  };

  const statusOptions = ["PENDING", "CONFIRMED", "PAID", "CHECKED_IN", "CANCELLED", "NO_SHOW"];

  function setFeedback(message, isError = false) {
    if (!feedback) {
      return;
    }
    feedback.textContent = message;
    feedback.style.color = isError ? "#9f403d" : "";
  }

  function buildQuery() {
    const params = new URLSearchParams();
    if (dateInput?.value) {
      params.set("date", dateInput.value);
    }
    if (customerNameInput?.value?.trim()) {
      params.set("customerName", customerNameInput.value.trim());
    }
    if (languageSelect?.value) {
      params.set("guideLanguage", languageSelect.value);
    }
    return params.toString();
  }

  function formatDate(isoDate) {
    if (!isoDate) {
      return "-";
    }
    return isoDate.replaceAll("-", "/");
  }

  function formatTime(timeSlot) {
    if (timeSlot === "11:00") return "11:00 - 12:30";
    if (timeSlot === "13:00") return "13:00 - 14:30";
    if (timeSlot === "15:30") return "15:30 - 17:00";
    return timeSlot || "-";
  }

  function formatLanguage(language) {
    return language === "ja" ? "日本語" : language === "en" ? "English" : language || "-";
  }

  function createStatusBadge(status) {
    const badge = document.createElement("span");
    badge.className = `admin-badge reservation-status-badge ${classes[status] || ""}`.trim();
    badge.dataset.status = status;
    badge.textContent = labels[status] || status;
    return badge;
  }

  function createStatusEditor(reservation) {
    const wrapper = document.createElement("div");
    const select = document.createElement("select");
    const button = document.createElement("button");

    wrapper.className = "reservation-status-editor";
    select.className = "reservation-status-select";

    statusOptions.forEach((status) => {
      const option = document.createElement("option");
      option.value = status;
      option.textContent = labels[status];
      option.selected = reservation.reservationStatus === status;
      select.append(option);
    });

    button.type = "button";
    button.className = "btn btn-secondary reservation-status-save";
    button.textContent = "更新";
    button.addEventListener("click", () => updateStatus(reservation.reservationId, select.value, wrapper.closest("tr")));

    wrapper.append(select, button);
    return wrapper;
  }

  function renderReservations(reservations) {
    tableBody.replaceChildren();

    if (!reservations.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 12;
      cell.className = "admin-empty";
      cell.textContent = "条件に一致する予約はありません。";
      row.append(cell);
      tableBody.append(row);
      return;
    }

    reservations.forEach((reservation) => {
      const row = document.createElement("tr");
      row.dataset.reservationId = String(reservation.reservationId);

      const cells = [
        reservation.reservationCode || `SOA-${reservation.reservationId}`,
        formatDate(reservation.reservationDate),
        formatTime(reservation.timeSlot),
        reservation.customerName || "-",
        reservation.guestCount ?? "-",
        formatLanguage(reservation.guideLanguage),
        reservation.customerEmail || "-",
        reservation.customerPhone || "-",
      ];

      cells.forEach((value) => {
        const cell = document.createElement("td");
        cell.textContent = String(value);
        row.append(cell);
      });

      const statusCell = document.createElement("td");
      statusCell.append(createStatusBadge(reservation.reservationStatus));
      row.append(statusCell);

      const editorCell = document.createElement("td");
      editorCell.append(createStatusEditor(reservation));
      row.append(editorCell);

      const noteCell = document.createElement("td");
      noteCell.textContent = reservation.notes || "-";
      row.append(noteCell);

      tableBody.append(row);
    });
  }

  async function loadReservations() {
    setFeedback("予約一覧を読み込んでいます...");

    try {
      const query = buildQuery();
      const response = await fetch(`/api/admin/reservations${query ? `?${query}` : ""}`);
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "予約一覧の取得に失敗しました。");
      }

      const payload = await response.json();
      renderReservations(payload.reservations || []);
      if (countBadge) {
        countBadge.textContent = `${payload.totalCount || 0}件`;
      }
      setFeedback(`${payload.totalCount || 0}件の予約を表示しています。`);
    } catch (error) {
      renderReservations([]);
      if (countBadge) {
        countBadge.textContent = "0件";
      }
      setFeedback(error.message || "予約一覧の取得に失敗しました。", true);
    }
  }

  async function updateStatus(reservationId, reservationStatus, row) {
    setFeedback(`${reservationId} のステータスを更新しています...`);

    try {
      const response = await fetch(`/api/admin/reservations/${reservationId}/status`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ reservationStatus }),
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "ステータス更新に失敗しました。");
      }

      const updated = await response.json();
      const badgeCell = row?.children?.[8];
      if (badgeCell) {
        badgeCell.replaceChildren(createStatusBadge(updated.reservationStatus));
      }
      setFeedback(`${updated.reservationCode || `SOA-${updated.reservationId}`} のステータスを ${labels[updated.reservationStatus] || updated.reservationStatus} に更新しました。`);
    } catch (error) {
      setFeedback(error.message || "ステータス更新に失敗しました。", true);
    }
  }

  function exportCsv() {
    const rows = [...tableBody.querySelectorAll("tr")];
    if (!rows.length || rows[0].querySelector(".admin-empty")) {
      setFeedback("出力できる予約データがありません。", true);
      return;
    }

    const header = ["予約番号", "日付", "時間", "氏名", "人数", "言語", "メール", "電話", "状態", "メモ"];
    const lines = [header.join(",")];

    rows.forEach((row) => {
      const cells = row.querySelectorAll("td");
      const record = [
        cells[0]?.textContent || "",
        cells[1]?.textContent || "",
        cells[2]?.textContent || "",
        cells[3]?.textContent || "",
        cells[4]?.textContent || "",
        cells[5]?.textContent || "",
        cells[6]?.textContent || "",
        cells[7]?.textContent || "",
        cells[8]?.textContent || "",
        cells[10]?.textContent || "",
      ].map((value) => `"${String(value).replaceAll("\"", "\"\"")}"`);
      lines.push(record.join(","));
    });

    const blob = new Blob(["\uFEFF" + lines.join("\n")], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.download = "reservations.csv";
    link.click();
    URL.revokeObjectURL(url);
    setFeedback("CSV を出力しました。");
  }

  searchButton?.addEventListener("click", (event) => {
    event.preventDefault();
    loadReservations();
  });

  exportButton?.addEventListener("click", (event) => {
    event.preventDefault();
    exportCsv();
  });

  loadReservations();
})();
