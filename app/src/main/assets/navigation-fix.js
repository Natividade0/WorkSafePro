// Corrige o botão Voltar do Android para navegar dentro do app antes de sair.
(function () {
  const getActiveSection = () => document.querySelector('.sec.on')?.id || 'dash';
  let currentSection = getActiveSection();
  let internalPop = false;

  const originalGo = window.go;
  const originalModal = window.modal;
  const originalCloseM = window.closeM;

  function setState(section, modalOpen) {
    const hash = modalOpen ? '#modal' : '#' + section;
    history.replaceState({ section, modal: !!modalOpen }, '', hash);
  }

  if (typeof originalGo === 'function') {
    window.go = function (id, push = true) {
      currentSection = id;
      originalGo(id);
      if (push && !internalPop) {
        history.pushState({ section: id, modal: false }, '', '#' + id);
      }
    };
  }

  if (typeof originalModal === 'function') {
    window.modal = function (title, body) {
      currentSection = getActiveSection();
      originalModal(title, body);
      if (!internalPop) {
        history.pushState({ section: currentSection, modal: true }, '', '#modal');
      }
    };
  }

  if (typeof originalCloseM === 'function') {
    window.closeM = function () {
      originalCloseM();
      if (history.state && history.state.modal) {
        history.replaceState({ section: currentSection, modal: false }, '', '#' + currentSection);
      }
    };
  }

  window.addEventListener('popstate', function (event) {
    internalPop = true;

    const modalEl = document.getElementById('modal');
    const isModalOpen = modalEl && modalEl.classList.contains('on');

    if (isModalOpen && typeof originalCloseM === 'function') {
      originalCloseM();
      internalPop = false;
      return;
    }

    const target = event.state && event.state.section ? event.state.section : 'dash';
    currentSection = target;

    if (typeof originalGo === 'function') {
      originalGo(target);
    }

    internalPop = false;
  });

  // Estado inicial: ao apertar voltar na primeira tela, o Android ainda pode sair do app.
  // Depois que o usuário navegar entre seções, ele volta pelas telas visitadas.
  if (!history.state) {
    history.replaceState({ section: currentSection, modal: false }, '', '#' + currentSection);
  } else {
    setState(currentSection, false);
  }
})();
