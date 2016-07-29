(function (document) {

  function ajaxPOST(url, cb) {
    const req = new XMLHttpRequest();
    req.open('POST', url, true);
    req.onreadystatechange = () => {
      if (req.readyState == 4) {
        if (req.status == 200) cb();
        else cb('error while sending HTTP request');
      }
    };
    req.send(null);
  }

  function runAction(action, id) {
    console.log('run action', action, 'with id', id);
    ajaxPOST('/api/actions/' + action + '?id=' + id, (err) => {
      if (err) console.error('Error while sending action');
    });
  }

  function init() {
    const actions = document.querySelectorAll('[data-action]');
    for (let i=0; i < actions.length; i++) {
      actions[i].onclick = (e) => {
        const target = e.target;
        const action = target.getAttribute('data-action');

        let id = undefined;
        {
          let it = target.parentElement;
          while (it) {
            id = it.getAttribute('data-id');
            if (id) break;
            it = it.parentElement;
          }
        }

        if (action && id) runAction(action, id);
      };
    }
  }

  if (document.readyState != 'loading') init();
  else document.addEventListener('DOMContentLoaded', init);

})(document);
