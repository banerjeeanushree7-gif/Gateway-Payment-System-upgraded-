(function () {
  'use strict';
  var page = document.body.dataset.page;
  function $(s, r) { return (r || document).querySelector(s); }
  function hex(n) { var a = new Uint8Array(n); crypto.getRandomValues(a); return Array.prototype.map.call(a, function (b) { return ('0' + b.toString(16)).slice(-2); }).join(''); }
  function el(tag, text, cls) { var e = document.createElement(tag); if (text != null) e.textContent = text; if (cls) e.className = cls; return e; }
  function luhn(s) { var sum = 0, alt = false; for (var i = s.length - 1; i >= 0; i--) { var n = +s[i]; if (alt) { n *= 2; if (n > 9) n -= 9; } sum += n; alt = !alt; } return sum % 10 === 0; }

  // Browser-side checks are for convenience only; PaymentServlet re-checks everything.
  var RULES = {
    amount: function (v) { return /^\d{1,7}(\.\d{1,2})?$/.test(v) && +v > 0 && +v <= 1e6 ? '' : 'Enter an amount from 0.01 to 1,000,000 (max 2 decimals).'; },
    name:   function (v) { return v.trim() && v.length <= 60 ? '' : 'Enter the name on the card.'; },
    pan:    function (v) { var d = v.replace(/[\s-]/g, ''); return /^\d{12,19}$/.test(d) && luhn(d) ? '' : 'Card number is not valid.'; },
    expiry: function (v) {
      var m = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(v.trim()); if (!m) return 'Use MM/YY.';
      var n = new Date(), y = n.getFullYear() % 100, mo = n.getMonth() + 1, yy = +m[2];
      return yy > y || (yy === y && +m[1] >= mo) ? '' : 'This card has expired.';
    },
    cvv: function (v) { var pan = ($('#cardNumber') || {}).value || ''; var amex = /^3[47]/.test(pan.replace(/\s/g, '')); return (amex ? /^\d{4}$/ : /^\d{3}$/).test(v) ? '' : (amex ? 'CVV must be 4 digits.' : 'CVV must be 3 digits.'); },
    upi:    function (v) { return /^[A-Za-z0-9._-]{2,64}@[A-Za-z]{2,32}$/.test(v.trim()) ? '' : 'UPI ID is not valid (example: name@okaxis).'; },
    bank:   function (v) { return v ? '' : 'Choose a bank.'; },
    acc:    function (v) { return /^\d{10,16}$/.test(v.trim()) ? '' : 'Account number must be 10 to 16 digits.'; },
    ifsc:   function (v) { return /^[A-Z]{4}0[A-Z0-9]{6}$/.test(v.trim().toUpperCase()) ? '' : 'IFSC is not valid (example: SBIN0123456).'; }
  };

  // CSRF token + one-time nonce for every POST form.
  fetch('api/csrf', { credentials: 'same-origin' }).then(function (r) { return r.json(); }).then(function (j) {
    document.querySelectorAll('form[method=post]').forEach(function (f) {
      [['_csrf', j.token], ['_nonce', hex(12)]].forEach(function (p) {
        var i = document.createElement('input'); i.type = 'hidden'; i.name = p[0]; i.value = p[1]; f.appendChild(i);
      });
    });
  });

  document.querySelectorAll('form[method=post]').forEach(function (f) {
    var err = $('.err', f);
    f.addEventListener('submit', function (e) {
      if (!f.elements._csrf) { e.preventDefault(); return; }
      var inputs = f.querySelectorAll('[data-rule]');
      for (var i = 0; i < inputs.length; i++) {
        var msg = RULES[inputs[i].dataset.rule](inputs[i].value);
        if (msg) { e.preventDefault(); if (err) err.textContent = msg; inputs[i].focus(); return; }
      }
      var b = $('button[type=submit]', f); if (b) b.disabled = true;   // stops double-clicks
    });
  });

  var pan = $('#cardNumber');
  if (pan) pan.addEventListener('input', function () {
    var d = pan.value.replace(/\D/g, '').slice(0, 19);
    pan.value = d.replace(/(.{4})/g, '$1 ').trim();
  });

  var q = new URLSearchParams(location.search);
  if (page === 'login') {
    var le = $('.err');
    if (q.get('error') === 'locked') le.textContent = 'Too many attempts. Try again in a few minutes.';
    else if (q.get('error')) le.textContent = 'Email or password is incorrect.';
  }


function pdfEsc(s){return String(s).replace(/[^\x20-\x7E]/g,' ').replace(/([\\()])/g,'\\$1')}
function makePdf(rows,foot){
  var c='0.357 0.204 0.839 rg 0 742 595 100 re f\n'+
    'BT 1 1 1 rg /F2 26 Tf 56 790 Td (Gateway) Tj ET\nBT 1 1 1 rg /F1 12 Tf 56 764 Td (Payment statement) Tj ET\n';
  var y=700;
  rows.forEach(function(r){
    c+='0.85 0.83 0.92 RG 56 '+(y-10)+' m 539 '+(y-10)+' l S\n';
    c+='BT 0.42 0.39 0.56 rg /F1 10 Tf 56 '+y+' Td ('+pdfEsc(r[0])+') Tj ET\n';
    c+='BT 0.11 0.09 0.25 rg /F2 12 Tf 200 '+y+' Td ('+pdfEsc(r[1])+') Tj ET\n';
    y-=32;
  });
  c+='BT 0.42 0.39 0.56 rg /F1 9 Tf 56 '+(y-10)+' Td ('+pdfEsc(foot)+') Tj ET\n';
  var o=['<</Type/Catalog/Pages 2 0 R>>','<</Type/Pages/Kids[3 0 R]/Count 1>>',
    '<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Contents 4 0 R/Resources<</Font<</F1 5 0 R/F2 6 0 R>>>>>>',
    '<</Length '+c.length+'>>\nstream\n'+c+'endstream',
    '<</Type/Font/Subtype/Type1/BaseFont/Helvetica/Encoding/WinAnsiEncoding>>',
    '<</Type/Font/Subtype/Type1/BaseFont/Helvetica-Bold/Encoding/WinAnsiEncoding>>'];
  var out='%PDF-1.4\n',off=[];
  o.forEach(function(b,i){off.push(out.length);out+=(i+1)+' 0 obj\n'+b+'\nendobj\n'});
  var x=out.length;out+='xref\n0 '+(o.length+1)+'\n0000000000 65535 f \n';
  off.forEach(function(n){out+=('0000000000'+n).slice(-10)+' 00000 n \n'});
  out+='trailer\n<</Size '+(o.length+1)+'/Root 1 0 R>>\nstartxref\n'+x+'\n%%EOF';
  return new TextEncoder().encode(out);
}

  var LABEL = { card: "Card", upi: "UPI", netbank: "Net banking" };
  function downloadStatement(p) {
    var rows = [["Status", "SUCCESS"], ["Transaction ID", p.txnId], ["Date and time", new Date(p.time).toLocaleString("en-IN")],
      ["Paid with", LABEL[p.method] || p.method], ["Account / ID", p.id], ["Amount", "INR " + Number(p.amount).toFixed(2)]];
    var bytes = makePdf(rows, "Simulated payment from Gateway. No money was moved.");
    var a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([bytes], { type: "application/pdf" }));
    a.download = "Gateway-statement-" + p.txnId.slice(0, 8) + ".pdf";
    document.body.appendChild(a); a.click(); a.remove();
  }

  function money(a) { return a == null ? '-' : Number(a).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }

  if (page === 'result') {
    var box = $('#result');
    if (q.get('error')) { box.appendChild(el('h2', 'Payment not recorded', 'bad')); box.appendChild(el('p', 'We could not reach the database. You were not charged; please try again.')); }
    else fetch('api/history?id=' + encodeURIComponent(q.get('id') || ''), { credentials: 'same-origin' })
      .then(function (r) { if (!r.ok) throw 0; return r.json(); })
      .then(function (p) {
        var ok = p.status === 'SUCCESS';
        box.appendChild(el('h2', ok ? 'Payment successful' : 'Payment failed', ok ? 'ok' : 'bad'));
        box.appendChild(el('p', ok ? 'Amount ' + money(p.amount) + ' paid with ' + p.method + ' (' + p.id + ').' : p.reason));
        box.appendChild(el('p', 'Transaction ' + p.txnId, 'muted'));
        if (ok) { var d = el('button', 'Download statement (PDF)'); d.type = 'button'; d.addEventListener('click', function () { downloadStatement(p); }); box.appendChild(d); }
      })
      .catch(function () { box.appendChild(el('h2', 'Transaction not found', 'bad')); });
  }

  if (page === 'history') {
    var tb = $('#rows');
    fetch('api/history', { credentials: 'same-origin' }).then(function (r) { return r.json(); }).then(function (j) {
      if (!j.payments.length) { var tr = el('tr'); var td = el('td', 'No payments yet.'); td.colSpan = 6; tr.appendChild(td); tb.appendChild(tr); }
      j.payments.forEach(function (p) {
        var tr = el('tr');
        [new Date(p.time).toLocaleString(), p.method, p.id, money(p.amount)].forEach(function (t) { tr.appendChild(el('td', t)); });
        tr.appendChild(el('td', p.status, p.status === 'SUCCESS' ? 'ok' : 'bad'));
        var pd = el('td'); if (p.status === 'SUCCESS') { var pb = el('button', 'PDF', 'link'); pb.type = 'button'; pb.addEventListener('click', function () { downloadStatement(p); }); pd.appendChild(pb); } tr.appendChild(pd);
        tb.appendChild(tr);
      });
    });
  }
})();
