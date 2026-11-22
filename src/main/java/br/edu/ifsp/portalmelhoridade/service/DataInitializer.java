package br.edu.ifsp.portalmelhoridade.service;

import br.edu.ifsp.portalmelhoridade.model.entity.EmergenciaEntity;
import br.edu.ifsp.portalmelhoridade.model.entity.ItemEntity;
import br.edu.ifsp.portalmelhoridade.model.entity.SecaoEntity;
import br.edu.ifsp.portalmelhoridade.repository.EmergenciaRepository;
import br.edu.ifsp.portalmelhoridade.repository.ItemRepository;
import br.edu.ifsp.portalmelhoridade.repository.SecaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializador de dados: popula o banco de dados na primeira execução quando
 * as tabelas estiverem vazias. Garante que os dados oficiais dos serviços
 * públicos de Hortolândia/SP e os canais de emergência estejam sempre disponíveis.
 *
 * <p>Em produção (PostgreSQL), os dados são inseridos uma única vez.
 * Em testes (H2 em memória), são recriados a cada execução.</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DATA  = "16/11/2026";
    private static final String PREF  = "https://www.hortolandia.sp.gov.br";

    private final SecaoRepository     secaoRepo;
    private final ItemRepository      itemRepo;
    private final EmergenciaRepository emergenciaRepo;

    public DataInitializer(SecaoRepository secaoRepo,
                           ItemRepository itemRepo,
                           EmergenciaRepository emergenciaRepo) {
        this.secaoRepo      = secaoRepo;
        this.itemRepo       = itemRepo;
        this.emergenciaRepo = emergenciaRepo;
    }

    @Override
    public void run(String... args) {
        if (secaoRepo.count() > 0) {
            logger.info("Banco já populado — DataInitializer ignorado.");
            return;
        }

        logger.info("Banco vazio — populando catálogo inicial...");

        popularSecoes();
        popularItens();
        popularEmergencias();

        logger.info("Catálogo populado com sucesso: {} seções, {} itens, {} emergências.",
                secaoRepo.count(), itemRepo.count(), emergenciaRepo.count());
    }

    // ─── Seções ────────────────────────────────────────────────────────────────

    private void popularSecoes() {
        secaoRepo.save(new SecaoEntity("saude",     "Saúde e Bem-Estar",       "Cuide da sua saúde com facilidade.",            "",   "health_and_safety", 0));
        secaoRepo.save(new SecaoEntity("direitos",  "Direitos e Benefícios",    "Conheça e faça valer seus direitos.",            "hi", "balance",           1));
        secaoRepo.save(new SecaoEntity("emergencia","Canais de Emergência",     "A ajuda que você precisa, quando precisar.",    "em", "warning",            2));
        secaoRepo.save(new SecaoEntity("lazer",     "Transporte e Lazer",       "Mobilidade, convivência e bem-estar.",           "ok", "directions_bus",    3));
    }

    // ─── Itens por seção ───────────────────────────────────────────────────────

    private void popularItens() {

        // Saúde
        itemRepo.save(new ItemEntity("saude", "medical_services",
                "UPA 24h Jardim Amanda",
                "Atendimento médico de urgência e emergência 24 horas no Jardim Amanda",
                "Unidade de Pronto Atendimento 24 horas Pastor Eneas de Castro Gama, equipada para acolhimento clínico adulto e pediátrico, suturas, exames de urgência, estabilização e medicação imediata para a região do Amanda.",
                "Rua Washington Luís, nº 1.066, Jardim Amanda II, Hortolândia - SP",
                "(19) 3897-4485",
                "Atendimento 24 horas todos os dias",
                PREF, DATA, 0));

        itemRepo.save(new ItemEntity("saude", "local_hospital",
                "UPA 24h Jardim Rosolém",
                "Pronto atendimento médico 24 horas para urgências e emergências clínicas",
                "Unidade de Pronto Atendimento 24 horas José Pereira de Amorim com equipe médica de plantão ininterrupto, leitos de observação, exames laboratoriais rápidos e retaguarda do Hospital Municipal Mário Covas.",
                "Rua Orlando Pavan, nº 395, Jardim Rosolém, Hortolândia - SP",
                "(19) 3845-1009",
                "Atendimento 24 horas todos os dias",
                PREF, DATA, 1));

        itemRepo.save(new ItemEntity("saude", "vaccines",
                "Salas de Vacinação nas UBSs de Hortolândia",
                "Vacinas gratuitas do SUS para idosos (Gripe, Covid-19 e Pneumocócica)",
                "Vacinação de rotina e campanhas anuais gratuitas disponíveis em todas as 17 UBSs municipais. Imunização contra Gripe (Influenza), reforço contra Covid-19, Pneumocócica 23 e Dupla Adulto (tétano e difteria). Apresente documento com foto e carteira de vacinação.",
                "Salas de vacinação descentralizadas nas 17 UBSs e USFs de Hortolândia - SP",
                "(19) 3965-1400",
                "Segunda a sexta-feira, das 7h30 às 15h30",
                "https://www.gov.br/saude/pt-br/vacinacao", DATA, 2));

        itemRepo.save(new ItemEntity("saude", "calendar_month",
                "UBSs e Central de Regulação de Consultas",
                "Consultas de rotina, farmácia básica e encaminhamento para especialistas",
                "As Unidades Básicas de Saúde realizam consultas de clínica médica, pré-natal, acompanhamento contínuo de hipertensão e diabetes, curativos e dispensação de medicamentos do SUS. Consultas com especialistas são agendadas via Central de Regulação após consulta na UBS de referência.",
                "UBS Amanda I: Rua Almada Negreiros, 1299 | UBS Rosolém: Rua Osmar Antônio Meira, 300 - Hortolândia - SP",
                "(19) 3965-1400",
                "Segunda a sexta-feira, das 7h às 16h",
                PREF, DATA, 3));

        // Direitos
        itemRepo.save(new ItemEntity("direitos", "volunteer_activism",
                "CRAS Jardim Amanda e CadÚnico",
                "Cadastro Único, orientação social e inclusão em benefícios socioassistenciais",
                "Porta de entrada para programas sociais em Hortolândia. Realiza novo cadastro e atualização obrigatória do CadÚnico, emissão de declarações e acompanhamento familiar para munícipes da região do Amanda.",
                "Avenida Tarsila do Amaral, nº 540, Jardim Amanda II, Hortolândia - SP",
                "(19) 3909-3642",
                "Segunda a sexta-feira, das 8h às 16h",
                PREF, DATA, 0));

        itemRepo.save(new ItemEntity("direitos", "groups",
                "CRAS Jardim Rosolém",
                "Atendimento socioassistencial, CadÚnico e apoio comunitário no Rosolém",
                "Unidade de referência territorial da assistência social (Joel Alves Assunção) para moradores da grande região do Rosolém. Orienta sobre direitos socioassistenciais, programas de transferência de renda e tarifa social de energia e água.",
                "Rua Guido Rosolém, nº 177, Jardim Rosolém, Hortolândia - SP",
                "(19) 3809-3164",
                "Segunda a sexta-feira, das 8h às 16h",
                PREF, DATA, 1));

        itemRepo.save(new ItemEntity("direitos", "balance",
                "BPC/LOAS da Pessoa Idosa (INSS)",
                "Benefício assistencial de 1 salário mínimo mensal para idosos acima de 65 anos",
                "Garante um salário mínimo por mês para idosos com 65 anos ou mais cuja renda familiar por pessoa seja inferior a 1/4 do salário mínimo vigente. Não exige contribuição prévia ao INSS. Requer inscrição ativa no CadÚnico. Solicitação online pelo Meu INSS ou central 135.",
                "Solicitação digital via aplicativo Meu INSS ou atendimento agendado pelo telefone 135",
                "135",
                "Telefone 135: Segunda a sábado, das 7h às 22h | Meu INSS: 24 horas online",
                "https://www.gov.br/inss/pt-br/direitos-e-deveres/beneficios-assistenciais/bpc", DATA, 2));

        itemRepo.save(new ItemEntity("direitos", "shield",
                "Estatuto da Pessoa Idosa e Disque 100",
                "Lei Federal nº 10.741 que assegura prioridade e proteção integral",
                "Garante prioridade especial a partir dos 80 anos e atendimento preferencial para maiores de 60 anos em órgãos públicos, bancos e serviços de saúde. Em caso de negligência, violência física, financeira ou psicológica, denuncie gratuitamente pelo Disque 100 ou procure o CREAS municipal.",
                "CREAS Hortolândia: Rua Luiz Camilo de Camargo, nº 581, Centro, Hortolândia - SP",
                "100",
                "Disque 100: Atendimento 24 horas todos os dias | CREAS: Segunda a sexta, das 8h às 17h",
                "https://www.gov.br/mdh/pt-br/assuntos/pessoa-idosa", DATA, 3));

        // Lazer
        itemRepo.save(new ItemEntity("lazer", "directions_bus",
                "Cartão de Gratuidade no Transporte (Idoso/Sênior)",
                "Passe livre municipal (a partir de 60 anos) e Cartão BUS+ Sênior (EMTU/RMC)",
                "Em Hortolândia, pessoas a partir de 60 anos têm gratuidade garantida nos ônibus urbanos municipais via Bilhete Único Sênior. Idosos com 65 anos ou mais podem viajar apresentando diretamente o RG. Para linhas intermunicipais da EMTU/RMC, utilize o Cartão BUS+ Sênior emitido no posto credenciado.",
                "Terminal Metropolitano de Hortolândia: Av. Santana, nº 880, Parque Hortolândia - SP",
                "(19) 3865-8920",
                "Segunda a sexta-feira, das 8h às 17h",
                "https://busmais.com.br/sobre/atendimento/", DATA, 0));

        itemRepo.save(new ItemEntity("lazer", "volunteer_activism",
                "CCMI - Unidade Remanso Campineiro",
                "Centro de Convivência da Melhor Idade: atividades físicas e recreação",
                "Centro mantido pela Prefeitura de Hortolândia com atendimento gratuito para moradores com 60 anos ou mais. Oferece hidroginástica adaptada, ginástica preventiva, dança sênior, coral, artesanato, jogos de salão e passeios. Inscrições com RG, CPF, comprovante de endereço e atestado médico.",
                "Rua Euclides Pires de Assis, nº 200, Remanso Campineiro, Hortolândia - SP",
                "(19) 3819-7966",
                "Segunda a sexta-feira, das 8h às 17h",
                PREF, DATA, 1));

        itemRepo.save(new ItemEntity("lazer", "groups",
                "CCMI - Unidade Jardim Amanda",
                "Polo regional de convivência, esportes adaptados e oficinas culturais no Amanda",
                "Segunda unidade do CCMI em Hortolândia, facilitando o acesso da população idosa do Jardim Amanda e região a aulas de dança, condicionamento físico suave, artesanato, palestras de saúde preventiva e integração comunitária.",
                "Avenida Tarsila do Amaral, nº 640, Jardim Amanda II, Hortolândia - SP",
                "(19) 3887-4644",
                "Segunda a sexta-feira, das 8h às 17h",
                PREF, DATA, 2));

        itemRepo.save(new ItemEntity("lazer", "theater_comedy",
                "Parque Socioambiental Irmã Dorothy Stang",
                "Pista plana de caminhada, academia ao ar livre e contato com a natureza",
                "Espaço público ecológico da Prefeitura de Hortolândia com pistas pavimentadas e planas para caminhadas seguras, aparelhos de ginástica ao ar livre próprios para idosos, hortas comunitárias educativas e ambiente acolhedor e arborizado.",
                "Rua Manoel Antonio da Silva, nº 415, Jardim Nossa Senhora de Fátima, Hortolândia - SP",
                "(19) 3845-1149",
                "Aberto diariamente das 6h às 18h",
                PREF, DATA, 3));
    }

    // ─── Emergências ───────────────────────────────────────────────────────────

    private void popularEmergencias() {
        emergenciaRepo.save(new EmergenciaEntity("medical_services",    "SAMU",                      "192", 0));
        emergenciaRepo.save(new EmergenciaEntity("local_fire_department","Bombeiros",                 "193", 1));
        emergenciaRepo.save(new EmergenciaEntity("local_police",         "Polícia Militar",           "190", 2));
        emergenciaRepo.save(new EmergenciaEntity("shield",               "Disque Direitos Humanos",   "100", 3));
        emergenciaRepo.save(new EmergenciaEntity("warning",              "Defesa Civil de Hortolândia","199", 4));
    }
}
