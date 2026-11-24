/* Portal da Melhor Idade - Interface acessível para a pessoa idosa em Hortolândia/SP.
   O conteúdo vem do servidor Java (CatalogoService, injetado em window.PMI_CATALOGO).
   O comando do assistente é interpretado e validado no servidor (/api/agente);
   aqui executamos as chamadas autorizadas e gerenciamos síntese de voz, telas e preferências. */
(function(){
"use strict";
var $=function(s){return document.querySelector(s)},app=$("#app"),root=document.documentElement;
var st={size:0,alto:false,leitura:false,lh:1,fonte:"atk",tema:"auto"},hist=[],cur="home";
var LH=[1.4,1.55,1.9];
function clamp(n,a,b){return Math.max(a,Math.min(b,n))}
function save(){try{localStorage.setItem("pmi2",JSON.stringify(st))}catch(e){}}
function load(){try{var s=JSON.parse(localStorage.getItem("pmi2")||"null");if(s){st.size=clamp(s.size|0,-2,4);st.alto=!!s.alto;st.leitura=!!s.leitura;st.lh=clamp(s.lh|0,0,2);st.fonte=s.fonte==="sis"?"sis":"atk";st.tema=["auto","claro","escuro"].indexOf(s.tema)>-1?s.tema:"auto"}}catch(e){}}
function apply(){root.style.fontSize=(16+st.size*2)+"px";
 if(st.alto)root.setAttribute("data-contrast","alto");else root.removeAttribute("data-contrast");
 if(st.tema==="claro")root.setAttribute("data-theme","light");else if(st.tema==="escuro")root.setAttribute("data-theme","dark");else root.removeAttribute("data-theme");
 root.style.setProperty("--lh",LH[st.lh]);
 root.style.setProperty("--ff",st.fonte==="atk"?'"Atkinson Hyperlegible",Verdana,sans-serif':"system-ui,Verdana,sans-serif")}
function esc(s){return String(s).replace(/[&<>"]/g,function(c){return {"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]})}
function ic(n){
 if(n==="directions_bus")return '<span class="ms" aria-hidden="true"><svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 17V6.5C5 4.6 6.6 3 8.5 3h7C17.4 3 19 4.6 19 6.5V17H5Z"/><path d="M5 11h14M8 7h8M7.5 17v2M16.5 17v2"/><circle cx="8" cy="17" r="1"/><circle cx="16" cy="17" r="1"/></svg></span>';
 var mapa={call:"☎",person:"●",volunteer_activism:"♥",verified:"✓",volume_up:"🔊",
 arrow_back:"←",accessibility_new:"♿",chevron_right:"›",search:"⌕",contrast:"◐",
 dark_mode:"☾",health_and_safety:"✚",balance:"⚖",warning:"⚠",
 support_agent:"☏",mic:"🎙",smart_toy:"✦",school:"▣",supervisor_account:"♧",psychology:"✦"};
 return '<span class="ms" aria-hidden="true">'+(mapa[n]||"•")+'</span>'
}

/* ---------- Feedback Háptico (Vibração no Celular) ---------- */
function vibrar(ms){
 if("vibrate" in navigator){
  try{ navigator.vibrate(ms||40); }catch(e){}
 }
}

/* ---------- Voz Acolhedora e Pausada (0.88x) ---------- */
function falar(t){
 var s=$("#status");if(!("speechSynthesis" in window)){if(s)s.textContent="Seu navegador não lê em voz alta. O texto está na tela.";return false}
 speechSynthesis.cancel();
 var u=new SpeechSynthesisUtterance(t);
 u.lang="pt-BR";
 u.rate=0.88; // Ritmo pausado e dicção calma para idosos
 u.pitch=1.0;
 speechSynthesis.speak(u);
 return true;
}

/* ---------- Dados (vêm do servidor Java) ---------- */
var CATALOGO=window.PMI_CATALOGO||{secoes:[],emergencias:[]};
var D={};
CATALOGO.secoes.forEach(function(x){
 var o={t:x.titulo,s:x.subtitulo,c:x.cor,i:x.icone};
 if(x.itens&&x.itens.length)o.it=x.itens.map(function(y){
  return {
   ic:y.icone,
   t:y.titulo,
   d:y.descricao,
   b:y.texto,
   end:y.endereco||"",
   tel:y.telefone||"",
   hor:y.horario||"",
   fon:y.fonteOficial||"",
   dt:y.dataAtualizacao||""
  };
 });
 D[x.id]=o});
if(D.emergencia)D.emergencia.em=CATALOGO.emergencias.map(function(e){return [e.icone,e.nome,e.numero]});
var ALL=[];Object.keys(D).forEach(function(k){if(D[k].it)D[k].it.forEach(function(x,j){ALL.push({k:k,j:j,ic:x.ic,t:x.t,d:x.d,b:x.b,end:x.end,tel:x.tel,hor:x.hor,fon:x.fon,dt:x.dt})})});

/* ---------- Telas ---------- */
function hdr(k,t,s,c){return '<header class="hdr '+c+'"><button class="ib" data-go="back" aria-label="Voltar">'+ic("arrow_back")+'</button><div><h1 tabindex="-1">'+esc(t)+'</h1><p>'+esc(s)+'</p></div><button class="ib" data-go="acess" aria-label="Acessibilidade e preferências">'+ic("accessibility_new")+'</button></header>'}
function itemBtn(x,semEmoji){return '<li><button class="item" data-det="'+x.k+':'+x.j+'">'+(semEmoji?'<span class="ms" aria-hidden="true">•</span>':ic(x.ic))+'<span class="tx"><b>'+esc(x.t)+'</b><small>'+esc(x.d)+'</small></span>'+ic("chevron_right")+'</button></li>'}
var V={
home:function(){return '<div class="body"><div class="brand"><img src="/img/logo.png" alt="Logo Portal da Melhor Idade" style="width:72px;height:72px;border-radius:18px;margin-bottom:8px;box-shadow:0 6px 16px rgba(0,0,0,0.15);object-fit:cover"><h1 tabindex="-1">Portal da<br>Melhor Idade</h1></div><p>Serviços públicos e informações oficiais para a pessoa idosa em Hortolândia/SP.</p>'+
 '<button class="search" data-go="busca"><span>Buscar serviços…</span>'+ic("search")+'</button>'+
 '<div class="abar" role="group" aria-label="Barra de acessibilidade rápida"><button data-a="menos" aria-label="Diminuir o tamanho das letras">A−</button><button data-a="mais" aria-label="Aumentar o tamanho das letras">A+</button><button data-a="contr" aria-label="Ativar ou desativar alto contraste">'+ic("contrast")+' Contraste</button><button data-a="tema" aria-label="Alternar modo claro e escuro">'+ic("dark_mode")+' Claro/Escuro</button></div>'+
 '<div class="tiles"><button class="tile" data-go="saude">'+ic("health_and_safety")+'Saúde e Bem-Estar</button><button class="tile hi" data-go="direitos">'+ic("balance")+'Direitos e Benefícios</button><button class="tile em" data-go="emergencia">'+ic("warning")+'Emergência e Proteção</button><button class="tile ok" data-go="lazer">'+ic("directions_bus")+'Transporte e Lazer</button></div>'+
 '<p class="ctr"><b>Precisa de socorro imediato?</b></p><button class="sos" data-go="emergencia">'+ic("call")+'NÚMEROS DE EMERGÊNCIA</button>'+
 '<div class="info-card">'+ic("verified")+'<div><b>Informações Oficiais de Hortolândia/SP</b><p>Consulte serviços públicos municipais, estaduais e federais com segurança, comodidade e acessibilidade.</p></div></div></div>'},
lista:function(k){var d=D[k];return hdr(k,d.t,d.s,d.c)+'<div class="body"><ul class="list">'+d.it.map(function(x,j){return itemBtn({k:k,j:j,ic:x.ic,t:x.t,d:x.d})}).join("")+'</ul><p class="note"><b>Serviços públicos municipais.</b> Toque no serviço desejado para consultar telefones oficiais, endereços e horários de atendimento.</p></div>'},
emergencia:function(){var d=D.emergencia;return hdr("emergencia",d.t,d.s,"em")+'<div class="body"><ul class="list">'+d.em.map(function(x){return '<li class="item">'+ic(x[0])+'<span class="tx"><b>'+esc(x[1])+'</b><small>Número: '+esc(x[2])+'</small></span><a class="call" href="tel:'+esc(x[2])+'" aria-label="Ligar imediatamente para '+esc(x[1])+', número '+esc(x[2])+'">'+ic("call")+'</a></li>'}).join("")+'</ul><p class="ctr"><button class="primary" data-say="Canais de Emergência. SAMU 192. Bombeiros 193. Polícia Militar 190. Disque Direitos Humanos 100. Defesa Civil de Hortolândia 199.">'+ic("volume_up")+' Ouvir todos os números</button></p><div class="info-card">'+ic("support_agent")+'<div><b>Ligação Gratuita</b><p>Os números de emergência são gratuitos e funcionam 24 horas por dia, 7 dias por semana, a partir de qualquer telefone fixo ou celular.</p></div></div></div>'},
busca:function(){return hdr("busca","Buscar Serviços","Digite o que você procura.","")+'<div class="body"><label for="q" style="position:absolute;left:-999px">Buscar serviços</label><input id="q" type="text" placeholder="Digite o que você procura…" autocomplete="off"><div class="chips" role="group" aria-label="Categorias"><button data-cat="" aria-pressed="true">Todos</button><button data-cat="saude" aria-pressed="false">Saúde</button><button data-cat="direitos" aria-pressed="false">Direitos</button><button data-cat="lazer" aria-pressed="false">Transporte e Lazer</button><button data-go="emergencia">Emergência</button></div><h2>Serviços Encontrados</h2><ul class="list" id="res"></ul></div>'},
det:function(k,j){
 var x=D[k].it[j],c=D[k].c;
 var telClean=x.tel?x.tel.replace(/[^\d+]/g,""):"";
 var foneHtml=x.tel?'<dt>Telefone oficial</dt><dd><a class="link-tel" href="tel:'+esc(telClean)+'" aria-label="Ligar para '+esc(x.tel)+'">'+ic("call")+' <b>'+esc(x.tel)+'</b> <small>(Toque para ligar)</small></a></dd>':'';
 var endHtml=x.end?'<dt>Endereço</dt><dd>'+esc(x.end)+'<br><a class="link-ext" href="https://www.google.com/maps/search/?api=1&query='+encodeURIComponent(x.end+', Hortolândia - SP')+'" target="_blank" rel="noopener noreferrer">📍 Ver no Google Maps ↗</a></dd>':'';
 var horHtml=x.hor?'<dt>Horário de atendimento</dt><dd>'+esc(x.hor)+'</dd>':'';
 var fonHtml=x.fon?'<dt>Fonte oficial</dt><dd><a class="link-ext" href="' + esc(x.fon) + '" target="_blank" rel="noopener noreferrer">Acessar página oficial do órgão ↗</a></dd>':'';
 var dtHtml=x.dt?'<p class="note"><b>Dados oficiais conferidos.</b><br>Última verificação: '+esc(x.dt)+'</p>':'';
 var textoFala=x.t+'. '+x.b+(x.end?'. Endereço: '+x.end:'')+(x.hor?'. Horário: '+x.hor:'')+(x.tel?'. Telefone: '+x.tel:'');
 return hdr("det","Detalhes do Serviço",D[k].t,c)+'<div class="body"><div class="item" style="cursor:default">'+ic(x.ic)+'<span class="tx"><b>'+esc(x.t)+'</b><small>'+esc(x.d)+'</small></span></div><h2>Sobre o serviço</h2><p id="dtxt">'+esc(x.b)+'</p><p><button class="primary" data-say="'+esc(textoFala)+'">'+ic("volume_up")+' Ouvir informações completas</button></p><dl>'+endHtml+foneHtml+horHtml+fonHtml+'</dl>'+dtHtml+'</div>';
},
acess:function(){function seg(a,l,p){return '<button data-'+a+'="'+l[0]+'" aria-pressed="'+(p===l[0])+'">'+l[1]+'</button>'}
 return hdr("acess","Acessibilidade e Preferências","Ajuste o portal para o seu maior conforto visual e de leitura.","")+'<div class="body"><h2>Tamanho das letras</h2><div class="seg"><button data-a="menos" aria-label="Diminuir letras">A−</button><button data-a="zero" aria-pressed="'+(st.size===0)+'" aria-label="Tamanho padrão">Padrão</button><button data-a="mais" aria-label="Aumentar letras">A+</button></div>'+
 '<div class="row2"><span id="l1">Modo Alto Contraste</span><button class="sw" role="switch" aria-labelledby="l1" aria-checked="'+st.alto+'" data-a="contr"></button></div>'+
 '<h2>Cores da tela (Tema)</h2><div class="seg"><button data-tema="auto" aria-pressed="'+(st.tema==="auto")+'">Automático</button><button data-tema="claro" aria-pressed="'+(st.tema==="claro")+'">Claro</button><button data-tema="escuro" aria-pressed="'+(st.tema==="escuro")+'">Escuro</button></div>'+
 '<div class="row2"><span id="l2">Leitura em voz alta automática (lê o conteúdo ao abrir a tela)</span><button class="sw" role="switch" aria-labelledby="l2" aria-checked="'+st.leitura+'" data-a="leitura"></button></div>'+
 '<h2>Espaçamento entre as linhas</h2><div class="seg"><button data-lh="0" aria-pressed="'+(st.lh===0)+'">Padrão</button><button data-lh="1" aria-pressed="'+(st.lh===1)+'">Médio</button><button data-lh="2" aria-pressed="'+(st.lh===2)+'">Amplo</button></div>'+
 '<h2><label for="fs">Tipo de letra (Fonte)</label></h2><select id="fs"><option value="atk"'+(st.fonte==="atk"?" selected":"")+'>Atkinson Hyperlegible (Letra de alta legibilidade)</option><option value="sis"'+(st.fonte==="sis"?" selected":"")+'>Letra padrão do aparelho</option></select></div>'},
ajuda:function(){return hdr("ajuda","Assistente Guia","Diga ou escreva o que você precisa.","")+'<div class="body"><p>Fale pelo microfone ou escreva no campo abaixo o que você deseja encontrar. Ao enviar, o texto poderá ser processado pelo Google Gemini quando a chave estiver configurada; sem ela, será usado o modo simulado local.</p><label for="cmd" style="position:absolute;left:-999px">O que você precisa?</label><input id="cmd" type="text" maxlength="300" placeholder="Escreva aqui o que você procura…" autocomplete="off"><div class="row" style="margin-top:10px"><button class="primary" id="env">Enviar</button><button class="primary" id="mic" style="display:none">'+ic("mic")+' Falar</button></div>'+
 '<div class="chips" aria-label="Exemplos comuns para testar"><button data-ex="Quero ver os postos de vacinação, mas com letras bem grandes">Vacinação + letras grandes</button><button data-ex="Tá muito claro, não tô enxergando direito">Muito claro (contraste)</button><button data-ex="Lê pra mim a parte dos direitos">Ler direitos</button><button data-ex="Me ajuda aí">Preciso de ajuda</button></div>'+
 '<p id="status" role="status" aria-live="polite"></p><div id="fa01" hidden><p><b>Não tenho certeza do que você procura. Escolha uma das opções abaixo:</b></p><div class="row"><button class="primary" data-ex="aumentar as letras">Aumentar letras</button><button class="primary" data-ex="alto contraste">Alto contraste</button><button class="primary" data-ex="ver saúde">Ver saúde</button><button class="primary" data-ex="emergência">Emergência</button></div></div>'+
 '<details><summary>Ver detalhes técnicos do assistente</summary><pre id="log">'+esc(LOG||"Nenhuma chamada registrada.")+'</pre></details><div class="info-card">'+ic("smart_toy")+'<div><b>Assistente Inteligente e Seguro</b><p>O Assistente interpreta pedidos em linguagem natural e executa apenas ações seguras previamente autorizadas.</p></div></div></div>'},
sobre:function(){return hdr("sobre","Sobre o Portal","Conheça a iniciativa e a equipe","")+'<div class="body">'+
 '<div class="info-card">'+ic("volunteer_activism")+'<div><b>Nosso Propósito</b><p>O <b>Portal da Melhor Idade</b> foi criado para promover autonomia, inclusão digital e cidadania para as pessoas idosas de Hortolândia e região, reunindo informações essenciais de saúde, direitos e convivência em um ambiente acolhedor e acessível.</p></div></div>'+
 '<h2>Vínculo Institucional</h2>'+
 '<div class="info-card">'+ic("school")+'<div><b>Instituto Federal de São Paulo (IFSP) – Campus Hortolândia</b><p>Projeto de extensão acadêmica desenvolvido na disciplina de <i>Prática em Sociedade e Informática III</i>, do curso de Tecnologia em Análise e Desenvolvimento de Sistemas.</p></div></div>'+
 '<h2>Equipe de Desenvolvimento</h2>'+
 '<ul class="team-list">'+
 '<li class="team-card destaque">'+ic("person")+'<div><b>Tabata Vitória Martins de Jesus</b><span>Desenvolvimento de Software, Arquitetura e Acessibilidade</span></div></li>'+
 '<li class="team-card">'+ic("person")+'<div><b>Luis Fernando Esteves Quintela</b><span>Pesquisa, Levantamento de Requisitos e Desenvolvimento</span></div></li>'+
 '<li class="team-card">'+ic("person")+'<div><b>Sarah Alves Soares</b><span>Pesquisa e Design de Experiência do Usuário (UX)</span></div></li>'+
 '<li class="team-card">'+ic("supervisor_account")+'<div><b>Prof. Carlos Eduardo Pagani</b><span>Orientação e Coordenação Pedagógica</span></div></li>'+
 '</ul>'+
 '<h2>Compromisso com a Acessibilidade</h2>'+
 '<div class="info-card">'+ic("verified")+'<div><b>Diretrizes Internacionais e Nacionais</b><p>Desenvolvido com base nas recomendações da <b>WCAG 2.1 (níveis A e AA)</b> e no <b>e-MAG</b> (Modelo de Acessibilidade em Governo Eletrônico), incorporando tipografia voltada para baixa visão (Atkinson Hyperlegible), alto contraste, leitura por voz e navegação assistida.</p></div></div>'+
 '<h2>Inteligência Artificial e Segurança</h2>'+
 '<div class="info-card">'+ic("psychology")+'<div><b>Assistente com Gemini e modo simulado</b><p>O assistente pode utilizar o Google Gemini para interpretar comandos. Se a chave não estiver configurada ou o serviço falhar, o modo simulado local <b>LlmSimulado</b> será usado. As ações passam por validação antes de serem executadas. A interpretação automática pode falhar; confira as informações oficiais e não use o assistente para orientações médicas ou emergências.</p></div></div>'+
 '</div>'}};
var LOG="";

/* ---------- Navegação ---------- */
function go(n,a,b,back){
 if(n==="ajuda"){toggleChat(true);return true;}
 if(!back&&cur!==n+(a?":"+a+":"+b:"")&&cur)hist.push(cur);
 var html,key=n;
 if(n==="home")html=V.home();else if(n==="saude"||n==="direitos"||n==="lazer")html=V.lista(n);
 else if(n==="det"){html=V.det(a,b);key="det:"+a+":"+b}
 else if(V[n])html=V[n]();else return false;
 cur=key;app.innerHTML=html;window.scrollTo(0,0);
 var nav={home:"home",busca:"busca",ajuda:"ajuda",sobre:"sobre"}[n];
 document.querySelectorAll(".nav button").forEach(function(b){if(b.dataset.go===nav)b.setAttribute("aria-current","page");else b.removeAttribute("aria-current")});
 var h=app.querySelector("h1");if(h)h.focus({preventScroll:true});
 if(n==="busca")filtrar("","");
 if(n==="ajuda")ligaMic();
 if(st.leitura&&n!=="ajuda"){var hh=app.querySelector("h1"),p=app.querySelector("header p,.body>p");falar((hh?hh.textContent:"")+". "+(p?p.textContent:""))}
 return true}
function voltar(){var p=hist.pop();if(!p){go("home",0,0,true);return}var m=p.split(":");go(m[0],m[1],m[2],true)}

/* ---------- Busca ---------- */
var catAtual="";
function filtrar(q,c){catAtual=c;var t=q.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g,"");
 var r=ALL.filter(function(x){var s=(x.t+" "+x.d+" "+x.b).toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g,"");return(!c||x.k===c)&&(!t||s.indexOf(t)>-1)});
 var ul=$("#res");if(ul)ul.innerHTML=r.length?r.map(function(x){return itemBtn(x,true)}).join(""):'<li class="note">Nenhum serviço encontrado.</li>'}

/* ---------- Ações de acessibilidade ---------- */
function ajusta(a){if(a==="menos")st.size=clamp(st.size-1,-2,4);else if(a==="mais")st.size=clamp(st.size+1,-2,4);else if(a==="zero")st.size=0;
 else if(a==="tema"){var esc2=st.tema==="escuro"||(st.tema==="auto"&&matchMedia("(prefers-color-scheme:dark)").matches);st.tema=esc2?"claro":"escuro"}else if(a==="contr")st.alto=!st.alto;else if(a==="leitura")st.leitura=!st.leitura;
 apply();save();if(cur==="acess"){var t=cur;app.innerHTML=V.acess();var h=app.querySelector("h1");if(h)h.focus({preventScroll:true})}}

/* ---------- Agente: o servidor Java interpreta e valida; aqui só executamos ---------- */
var TOOLS={
 ajustarFonte:function(a){st.size=clamp(st.size+a.delta,-2,4);apply();save();return{sucesso:true,novoNivelFonte:st.size}},
 alterarContraste:function(a){st.alto=!!a.ativo;apply();save();return{sucesso:true,altoContrasteAtivo:st.alto}},
 navegarParaSecao:function(a){var ok=go(a.idSecao);return{sucesso:ok,secaoAtual:a.idSecao}},
 executarLeituraVoz:function(a){var d=D[a.idSecao],t=d.t+". "+d.s+" "+(d.it?d.it.map(function(x){return x.t+". "+x.b}).join(" "):d.em.map(function(x){return x[1]+" "+x[2]}).join(". "));return{sucesso:true,status:falar(t)?"lendo":"sem voz"}}};
/* Defesa em profundidade: mesmo vindo do servidor, o navegador só executa o que está na whitelist. */
function permitida(c){if(!c||!Object.prototype.hasOwnProperty.call(TOOLS,c.name))return"ferramenta fora da whitelist";var a=c.arguments||{};
 if(c.name==="ajustarFonte"&&!Number.isInteger(a.delta))return"delta inválido";
 if(c.name==="alterarContraste"&&typeof a.ativo!=="boolean")return"parâmetro inválido";
 if((c.name==="navegarParaSecao"||c.name==="executarLeituraVoz")&&!Object.prototype.hasOwnProperty.call(D,a.idSecao))return"seção inexistente";return null}
function logar(x){LOG=(LOG?LOG+"\n":"")+x;var l=$("#log");if(l){l.textContent=LOG;l.scrollTop=l.scrollHeight}}
function say(m){var s=$("#status");if(s)s.textContent=m}
function fa01(m){say(m);var f=$("#fa01");if(f)f.hidden=false;falar("Não entendi com certeza. Escolha uma das opções na tela.");addChatMsg(m||"Não entendi com certeza. Escolha uma das opções abaixo.",false)}

/* Adiciona balão de mensagem ao chat flutuante */
function addChatMsg(texto,isUser,audioMsg,isEmergency){
 var msgs=$("#chat-msgs");if(!msgs)return;
 var b=document.createElement("div");b.className="chat-bubble "+(isUser?"user":"bot");
 var p=document.createElement("p");p.textContent=texto;b.appendChild(p);
 if(!isUser&&audioMsg){
  var btn=document.createElement("button");btn.className="chat-audio-btn";
  btn.setAttribute("aria-label","Ouvir esta mensagem em voz alta");
  btn.innerHTML="🔊 Ouvir";
  btn.onclick=function(){falar(audioMsg)};
  b.appendChild(btn);
 }
 if(!isUser&&isEmergency){
  var emerDiv=document.createElement("div");
  emerDiv.className="chat-emergency-actions";
  emerDiv.innerHTML='<a href="tel:192" class="chat-call-btn samu" aria-label="Ligar para o SAMU no 192">📞 Ligar 192 (SAMU)</a>'+
                    '<a href="tel:193" class="chat-call-btn bombeiros" aria-label="Ligar para os Bombeiros no 193">🚒 Ligar 193 (Bombeiros)</a>';
  b.appendChild(emerDiv);
 }
 msgs.appendChild(b);msgs.scrollTop=msgs.scrollHeight;
}

function toggleChat(abrir){
 var m=$("#chat-modal"),f=$("#fab-chat"),bk=$("#chat-backdrop");if(!m)return;
 var show=typeof abrir==="boolean"?abrir:m.hidden;
 m.hidden=!show;
 vibrar(30);
 if(bk)bk.hidden=!show;
 if(f){
  f.setAttribute("aria-expanded",String(show));
  f.style.display=show?"none":"";
 }
 if(show){
  document.body.classList.add("chat-open");
  var inp=$("#chat-input");if(inp)setTimeout(function(){inp.focus()},150);
  var msgs=$("#chat-msgs");if(msgs)msgs.scrollTop=msgs.scrollHeight;
 } else {
  document.body.classList.remove("chat-open");
 }
}

var historicoChat=[];

function processar(txt,fromChat){
 txt=(txt||"").trim();if(!txt)return;
 vibrar(35);
 var f=$("#fa01");if(f)f.hidden=true;
 logar('> "'+txt+'"');
 addChatMsg(txt,true);
 historicoChat.push("Idoso: " + txt);
 if(historicoChat.length > 6) historicoChat.shift();

 var msgs=$("#chat-msgs");
 var typ=document.createElement("div");typ.id="chat-typing";typ.className="chat-bubble bot chat-typing";
 typ.innerHTML='<span class="dot"></span><span class="dot"></span><span class="dot"></span> <span>Pensando…</span>';
 if(msgs){msgs.appendChild(typ);msgs.scrollTop=msgs.scrollHeight}
 var badge=$("#chat-badge");if(badge)badge.textContent="✦ Pensando…";
 var ctrl=new AbortController(),timer=setTimeout(function(){ctrl.abort()},14000); /* 14 s para acomodar IA */
 fetch("/api/agente",{
  method:"POST",
  headers:{"Content-Type":"application/json"},
  body:JSON.stringify({texto:txt,nivelFonte:st.size,historico:historicoChat}),
  signal:ctrl.signal
 })
 .then(function(r){if(!r.ok)throw new Error("HTTP "+r.status);return r.json()})
 .then(function(resp){clearTimeout(timer);
  vibrar(45);
  var tEl=$("#chat-typing");if(tEl)tEl.remove();
  var isGemini=resp.origem&&resp.origem.toLowerCase().indexOf("gemini")>-1;
  if(badge)badge.textContent=isGemini?"✦ Google Gemini IA":"✦ Assistente Oficial";
  var msgSrv=resp.mensagem&&resp.mensagem.trim()?resp.mensagem:null;
  logar("  provedor: "+(resp.origem||"IA"));
  if(resp.fa01){
   logar("  confiança "+resp.confianca+" < 0,80 → FA01 (nenhuma ferramenta executada)");
   var msgFa01=msgSrv||"Não entendi com certeza. Escolha uma opção abaixo.";
   fa01(msgFa01);return;
  }
  (resp.bloqueadas||[]).forEach(function(b){logar("  ✗ bloqueada no servidor: "+b)});
  var n=0;
  var navegou=false;
  var isEmerg=false;
  (resp.chamadas||[]).forEach(function(c){
   var e=permitida(c);if(e){logar("  ✗ "+JSON.stringify(c)+" bloqueada: "+e);return}
   var res=TOOLS[c.name](c.arguments);logar("  ✓ "+JSON.stringify(c)+" → "+JSON.stringify(res));n++;
   if(c.name==="navegarParaSecao"&&res&&res.sucesso){
    if(c.arguments&&c.arguments.idSecao==="emergencia") isEmerg=true;
    else navegou=true;
   }
  });
  var txtLower=txt.toLowerCase();
  if(txtLower.indexOf("socorr")>-1||txtLower.indexOf("emergenc")>-1||txtLower.indexOf("samu")>-1||txtLower.indexOf("passando mal")>-1||txtLower.indexOf("caiu")>-1){
   isEmerg=true;
  }
  var textoFinal = msgSrv || (n ? "Pronto! Ação realizada." : "Não consegui fazer essa ação.");
  historicoChat.push("Assistente: " + textoFinal);
  if(historicoChat.length > 6) historicoChat.shift();

  say(textoFinal);
  addChatMsg(textoFinal,false,textoFinal,isEmerg);
  falar(textoFinal);
  if(navegou && !isEmerg){
   setTimeout(function(){
    toggleChat(false);
   }, 900);
  }
 })
 .catch(function(){
  clearTimeout(timer);
  var tEl=$("#chat-typing");if(tEl)tEl.remove();
  if(badge)badge.textContent="✦ Assistente Conectado";
  logar("  falha ou tempo esgotado → FA01");
  fa01("Não consegui falar com o assistente agora. Use as opções abaixo.");
 });
}

var micRecognition=null;
function ligaMic(btnId,inputElId){
 var SR=window.SpeechRecognition||window.webkitSpeechRecognition,m=$(btnId);if(!m||!SR)return;
 m.style.display="";
 m.onclick=function(){
  vibrar(40);
  var banner=$("#chat-recording-banner");
  if(m.classList.contains("recording")){
   if(micRecognition){try{micRecognition.stop()}catch(e){}}
   return;
  }
  try{
   var r=new SR();
   micRecognition=r;
   r.lang="pt-BR";
   r.continuous=false;
   r.interimResults=false;
   m.classList.add("recording");
   m.title="Gravando... Toque para concluir";
   if(banner)banner.hidden=false;
   var inp=$(inputElId);
   var oldPlaceholder=inp?inp.placeholder:"";
   if(inp)inp.placeholder="🔴 Ouvindo... Fale agora!";

   function encerraGravacao(){
    m.classList.remove("recording");
    m.title="Falar pelo microfone";
    if(banner)banner.hidden=true;
    if(inp)inp.placeholder=oldPlaceholder;
    micRecognition=null;
   }

   var cancelBtn=$("#rec-cancel-btn");
   if(cancelBtn){
    cancelBtn.onclick=function(e){
     e.stopPropagation();
     try{r.abort()}catch(err){}
     encerraGravacao();
    };
   }

   r.onresult=function(e){
    encerraGravacao();
    var t=e.results[0][0].transcript;
    if(inp)inp.value=t;
    processar(t,true);
   };
   r.onerror=function(e){
    encerraGravacao();
    if(e.error!=="aborted"&&e.error!=="no-speech"){
     say("Não consegui ouvir. Tente digitar.");
    }
   };
   r.onend=function(){
    encerraGravacao();
   };
   r.start();
  }catch(x){
   m.classList.remove("recording");
   if(banner)banner.hidden=true;
   say("Microfone indisponível neste navegador.");
  }
 };
}

/* ---------- PWA: Service Worker e Banner de Instalação ---------- */
if("serviceWorker" in navigator){
 window.addEventListener("load",function(){
  navigator.serviceWorker.register("/sw.js").catch(function(e){console.warn("SW erro:",e)});
 });
}
var pwaDeferredPrompt=null;
// Esconde o banner automaticamente se o app já foi instalado (modo standalone/PWA)
if(window.matchMedia("(display-mode: standalone)").matches||window.navigator.standalone===true){
 var _b=$("#pwa-install-banner");if(_b)_b.hidden=true;
}
window.addEventListener("beforeinstallprompt",function(e){
 e.preventDefault();pwaDeferredPrompt=e;
 // Só mostra se não estiver em modo standalone
 if(!window.matchMedia("(display-mode: standalone)").matches&&window.navigator.standalone!==true){
  var banner=$("#pwa-install-banner");if(banner)banner.hidden=false;
 }
});
// Quando instalado com sucesso, esconde o banner permanentemente
window.addEventListener("appinstalled",function(){
 var banner=$("#pwa-install-banner");if(banner)banner.hidden=true;
 pwaDeferredPrompt=null;
});

/* ---------- Eventos ---------- */
document.addEventListener("click",function(e){
 var t=e.target.closest("button,a,div#chat-backdrop");if(!t)return;
 var d=t.dataset;
 if(t.id==="chat-backdrop"||t.id==="chat-close"){toggleChat(false);return}
 if(t.id==="fab-chat"||t.closest("#fab-chat")){toggleChat();return}
 if(t.id==="chat-send"){var ci=$("#chat-input");if(ci){processar(ci.value,true);ci.value=""}return}
 if(d.chatCmd){processar(d.chatCmd,true);return}
 if(t.id==="pwa-install-btn"){
  if(pwaDeferredPrompt){
   var instBanner=$("#pwa-install-banner");
   pwaDeferredPrompt.prompt();
   pwaDeferredPrompt.userChoice.then(function(r){
    pwaDeferredPrompt=null;
    if(instBanner)instBanner.hidden=true;
   });
  } else {
   var b=$("#pwa-install-banner");if(b)b.hidden=true;
  }
  return;
 }
 if(t.id==="pwa-dismiss-btn"){var b2=$("#pwa-install-banner");if(b2)b2.hidden=true;return}
 if(d.go){e.preventDefault();if(d.go==="back")voltar();else{hist.length=d.go==="home"?0:hist.length;go(d.go)}}
 else if(d.det){var p=d.det.split(":");go("det",p[0],+p[1])}
 else if(d.a)ajusta(d.a);
 else if(d.tema){st.tema=d.tema;apply();save();app.innerHTML=V.acess()}
 else if(d.lh!==undefined){st.lh=+d.lh;apply();save();app.innerHTML=V.acess()}
 else if(d.say)falar(d.say);
 else if(d.cat!==undefined){var q=$("#q").value;document.querySelectorAll("[data-cat]").forEach(function(b){b.setAttribute("aria-pressed",String(b===t))});filtrar(q,d.cat)}
 else if(d.ex){var c=$("#cmd");if(c)c.value=d.ex;processar(d.ex)}
 else if(t.id==="env")processar($("#cmd").value);
});
document.addEventListener("input",function(e){if(e.target.id==="q")filtrar(e.target.value,catAtual)});
document.addEventListener("change",function(e){if(e.target.id==="fs"){st.fonte=e.target.value;apply();save()}});
document.addEventListener("keydown",function(e){
 if(e.key==="Enter"){
  if(e.target.id==="cmd")processar(e.target.value);
  else if(e.target.id==="chat-input"){processar(e.target.value,true);e.target.value=""}
 }
 if(e.key==="Escape"){toggleChat(false)}
});
load();apply();var h0=(location.hash||"").slice(1);go(D[h0]||(V[h0]&&h0!=="det")?h0:"home");
ligaMic("#mic","#cmd");
ligaMic("#chat-mic","#chat-input");
})();
