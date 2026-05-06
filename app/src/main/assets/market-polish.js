// Camada visual inspirada em fluxo de apps SST de mercado, sem copiar identidade visual.
(function(){
  const $ = id => document.getElementById(id);

  function card(icon,title,sub,go){
    return '<button class="moduleCard" onclick="go(\''+go+'\')"><div class="mIcon">'+icon+'</div><div><b>'+title+'</b><span>'+sub+'</span></div><div class="chev">›</div></button>';
  }

  window.renderDash = function(){
    const ptAtivas = db.pts.filter(p=>p.status!='encerrada').length;
    const rncAbertas = db.rncs.filter(r=>r.status!='encerrada').length;
    const epiAlertas = db.epis.filter(e=>days(e.v)<=30||Number(e.est)<=Number(e.min)).length;
    $('dash').innerHTML =
      '<div class="hero"><h1>Segurança do Trabalho em campo</h1><p>Controle PT, inspeções, EPIs, trabalhadores, RNC e DDS de forma simples, offline e com PDF.</p></div>'+ 
      '<div class="quick">'+
        '<button onclick="openPT()"><span class="qicon">📝</span><span>Nova PT</span></button>'+ 
        '<button onclick="openInsp()"><span class="qicon">🔍</span><span>Inspeção</span></button>'+ 
        '<button onclick="openEntrega()"><span class="qicon">🦺</span><span>EPI</span></button>'+ 
        '<button onclick="openDDS()"><span class="qicon">💬</span><span>DDS</span></button>'+ 
        '<button onclick="go(\'rel\')"><span class="qicon">📄</span><span>PDF</span></button>'+ 
      '</div>'+ 
      '<div class="grid cards" style="margin-bottom:14px">'+
        '<div class="card kpi"><strong>'+ptAtivas+'</strong><span>PTs ativas</span></div>'+ 
        '<div class="card kpi"><strong>'+db.insps.length+'</strong><span>Inspeções</span></div>'+ 
        '<div class="card kpi"><strong>'+rncAbertas+'</strong><span>RNC abertas</span></div>'+ 
        '<div class="card kpi"><strong>'+epiAlertas+'</strong><span>Alertas EPI</span></div>'+ 
      '</div>'+ 
      '<div class="moduleList">'+
        card('📁','Documentação de Segurança','PT, DDS, RNC e relatórios em PDF','pt')+
        card('🔎','Inspeções de Segurança','Checklists flexíveis e evidências','insp')+
        card('🦺','Controle de Estoque e Entrega de EPIs','CA, estoque, entrega e assinatura','epi')+
        card('👷','Trabalhadores e Treinamentos','ASO, NRs e aptidão automática','trab')+
        card('⚠️','Gestão de Não Conformidade','Prazos, responsáveis e status','rnc')+
        card('📊','Relatórios e Backup','PDF gerencial e arquivo JSON','rel')+
      '</div>';
  };

  // Reaplica renderização com a nova tela inicial.
  setTimeout(()=>{ if(typeof render==='function') render(); },50);
})();
