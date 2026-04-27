(() => {
  const storageKey = "scent_of_asa_guide_login_id";
  const form = document.querySelector("#guide-login-form");
  const select = document.querySelector("#guide-login-id");
  const feedback = document.querySelector("#guide-login-feedback");

  if (!form || !select || !feedback) {
    return;
  }

  function setFeedback(message, isError = false) {
    feedback.textContent = message;
    feedback.style.color = isError ? "#9f403d" : "";
  }

  function getLanguageLabel(language) {
    return language === "en" ? "EN" : "JP";
  }

  async function loadGuides() {
    setFeedback("ガイド一覧を読み込んでいます。");
    try {
      const response = await fetch("/api/admin/guide-staff");
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "ガイド一覧の取得に失敗しました。");
      }

      const guides = await response.json();
      const remembered = window.localStorage.getItem(storageKey) || "";
      select.innerHTML = '<option value="">ガイドを選択してください</option>';
      guides.forEach((guide) => {
        const option = document.createElement("option");
        option.value = guide.loginId;
        option.textContent = `${guide.displayName} (${getLanguageLabel(guide.guideLanguage)})`;
        option.selected = guide.loginId === remembered;
        select.append(option);
      });
      setFeedback("本人のログインIDを選んでください。");
    } catch (error) {
      setFeedback(error.message || "ガイド一覧の取得に失敗しました。", true);
    }
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const loginId = select.value.trim();
    if (!loginId) {
      setFeedback("ログインIDを選択してください。", true);
      return;
    }

    window.localStorage.setItem(storageKey, loginId);
    window.location.href = `guide_schedule.html?loginId=${encodeURIComponent(loginId)}`;
  });

  loadGuides();
})();
