package com.test.ucount.recertapi.service;

import com.test.ucount.enums.ForgnOperations;
import com.test.ucount.enums.OriginationBusPercentEnum;
import com.test.ucount.recertapi.dto.*;
import com.test.ucount.recertapi.repository.*;
import com.test.ucount.shared.domain.*;
import com.test.ucount.shared.exception.UcsApiException;
import com.test.ucount.shared.util.Const;
import com.test.ucount.shared.util.DateUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.test.ucount.recertapi.util.RecertConstants;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private static final Logger logger = LogManager.getLogger(QuestionnaireServiceImpl.class);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

    @Value("${flagType.reviewFlagList}")
    List<String> reviewFlagList;

    @Value("${flagType.followupFlagList}")
    List<String> followupFlagList;

    @Autowired
    private RecertServiceHelper serviceHelper;

    @Autowired
    private Form16ServiceHelper form16ServiceHelper;

    @Autowired
    private UcsAddtnlCmntTranRepository ucsAddtnlCmntTranRepository;

    @Autowired
    private UcsApplQstnrRepository ucsApplQstnrRepository;

    @Autowired
    private UcsSvcgOperTranRepository ucsSvcgOperTranRepository;

    @Autowired
    private UcsStateRepository stateRepository;

    @Autowired
    private UcsOrignDcsnTranRepository ucsOrignDcsnTranRepository;

    @Autowired
    private UcsOrignTranRepository ucsOrignTranRepository;

    @Autowired
    private UcsOrignProdVolTransRepository ucsOrignProdVolTransRepository;

    @Autowired
    private UcsOrignProdTransRepository ucsOrignProdTransRepository;

    @Autowired
    private UcsExtnlAudtrRepository ucsExtnlAudtrRepository;

    @Autowired
    private UcsFinlInfoTransRepository ucsFinlInfoTransRepository;

    @Autowired
    private UcsOrgtnExtnlAudtrTransRepository ucsOrgtnExtnlAudtrTransRepository;

    @Autowired
    private UcsQstnrDcsnTransRepository ucsQstnrDcsnTransRepository;

    @Autowired
    private UcsFinlInfoDcsnTransRepository ucsFinlInfoDcsnTransRepository;

    @Autowired
    private UcsInsDscnTranRepository ucsInsDscnTranRepository;

    @Autowired
    private UcsOfacCmplncTranRepository ucsOfacCmplncTranRepository;

    @Autowired
    private UcsInvstrRepository ucsInvstrRepository;

    @Autowired
    private UcsOrgtnInvstrPtfTransRepository ucsOrgtnInvstrPtfTransRepository;

    @Autowired
    private UcsCntprtyAcctRoleAssnRepository ucsCntprtyAcctRoleAssnRepository;

    @Autowired
    private UcsOrgtnInvstrTransRepository ucsOrgtnInvstrTransRepository;

    @Autowired
    private UcsCntprtyAcctRepository ucsCntprtyAcctRepository;

    @Autowired
    private UcsintlOperTranRepository ucsintlOperTranRepository;

    @Autowired
    private UcsTpoOwnpTranRepository ucsTpoOwnpTranRepository;

    @Autowired
    private UcsTpoTranRepository ucsTpoTranRepository;

    @Autowired
    private UcsApplTrkgRepository ucsApplTrkgRepository;

    @Override
    public QuestionnaireDTO getQuestionnaireData(Integer orgId, Short dtfsclYr, Integer idApplTrkg,
            String appStatus, String formStatus, String applFormLink,
            String previousSection, String nextSection) {

        QuestionnaireDTO questionnaireDTO = new QuestionnaireDTO();
        UcsApplTrkg ucsApplTrkg = ucsApplTrkgRepository.findByIdApplTrkg(idApplTrkg);
        Date ucsApplTrkgCreateDate = ucsApplTrkg.getDttmCreate();

        boolean isPastCutOffDate = false;
        try {
            Date ofacCutOffDate = dateFormat.parse(Const.ACR_QUESTIONNAIRE_CUT_OFF_DATE);
            String tempDttmCreate = dateFormat.format(new Date(ucsApplTrkg.getDttmCreate().getTime()));
            Date dateCreate = dateFormat.parse(tempDttmCreate);
            if (dateCreate.compareTo(ofacCutOffDate) > 0) {
                isPastCutOffDate = true;
            } else {
                isPastCutOffDate = false;
            }
        } catch (ParseException parseException) {
            throw new UcsApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unparsable date: ", parseException);
        }

        List<QuestionnaireDecisionDTO> ofacDecisions;
        if (isPastCutOffDate) {
            ofacDecisions = getRoleBasedOfacQuestions(orgId, dtfsclYr, idApplTrkg, ucsApplTrkgCreateDate);
        } else {
            ofacDecisions = getOFACDecisions(orgId, dtfsclYr, idApplTrkg, ucsApplTrkgCreateDate);
        }
        questionnaireDTO.setOfacDecisions(ofacDecisions);

        List<UcsApplQstnr> changeReportQuestions = ucsApplQstnrRepository.findByCodeLike(Const.F16_QUES_CHG_REPQ1);
        UcsApplQstnr changeReportQ1 = changeReportQuestions.isEmpty() ? null : changeReportQuestions.get(0);
        List<UcsApplQstnr> changeReportQuestions2 = ucsApplQstnrRepository.findByCodeLike(Const.F16_QUES_CHG_REPQ2);
        UcsApplQstnr changeReportQ2 = changeReportQuestions2.isEmpty() ? null : changeReportQuestions2.get(0);

        List<QuestionnaireDecisionDTO> changeReportDecsns = new ArrayList<>();
        if (changeReportQ1 != null) {
            QuestionnaireDecisionDTO questionnaireDecisionDTO1 = new QuestionnaireDecisionDTO();
            questionnaireDecisionDTO1.setCdqstnrdscn(Const.F16_QUES_CHG_REPQ1);
            questionnaireDecisionDTO1.setQstnText(changeReportQ1.getDescQstnr().trim());
            changeReportDecsns.add(questionnaireDecisionDTO1);
        }

        if (changeReportQ2 != null) {
            QuestionnaireDecisionDTO questionnaireDecisionDTO2 = new QuestionnaireDecisionDTO();
            questionnaireDecisionDTO2.setCdqstnrdscn(Const.F16_QUES_CHG_REPQ2);
            questionnaireDecisionDTO2.setQstnText(changeReportQ2.getDescQstnr().trim());
            changeReportDecsns.add(questionnaireDecisionDTO2);
        }
        questionnaireDTO.setQuestionnaireDecisions(changeReportDecsns);

        List<UcsApplQstnr> infoSecList = ucsApplQstnrRepository.findByCodeLike(Const.F16_QUES_INFOSE);
        List<UcsQstnrDcsnTrans> ucsQstnrDcsnTransInfoSecObj = ucsQstnrDcsnTransRepository
                .findByNameQstnrSubject(Const.F16_Question_Compliance, Const.F16_QUES_INFOSE);

        ArrayList<QuestionnaireDecisionDTO> infoSecDecsnsList = new ArrayList<>();
        if (!ucsQstnrDcsnTransInfoSecObj.isEmpty()) {
            for (UcsApplQstnr infoSec : infoSecList) {
                QuestionnaireDecisionDTO infoSecQstnreDecisions = new QuestionnaireDecisionDTO();
                infoSecQstnreDecisions.setCdqstnrdscn(infoSec.getCdQstnr());
                infoSecQstnreDecisions.setQstnText(infoSec.getDescQstnr().trim());
                infoSecDecsnsList.add(infoSecQstnreDecisions);
            }
        }
        questionnaireDTO.setInfoSecQstnreDecisions(infoSecDecsnsList);

        if (questionnaireDTO.getInfoSecQstnreDecisions() == null) {
            questionnaireDTO.setInfoSecQstnreDecisions(infoSecDecsnsList);
        }

        QuestionnaireLookUpWrapperDTO questionnaireLookUpWrapperDTO = setUpQuestionnaireLookup();
        questionnaireDTO.setLookup(questionnaireLookUpWrapperDTO);

        List<QuestionnaireDecisionDTO> whlDecisions = getWHLDecisions(orgId, dtfsclYr, idApplTrkg);
        List<QuestionnaireDecisionDTO> miQuestionaire = getMIQuestionnaire(orgId, dtfsclYr, idApplTrkg, ucsApplTrkgCreateDate);
        questionnaireDTO.setMiCancellationQstns(miQuestionaire);
        questionnaireDTO.setWareHouseLendingFocusedQstns(whlDecisions);
        questionnaireDTO.setCancellationQstns(miQuestionaire);

        questionnaireDTO.setAppStatus(appStatus);
        questionnaireDTO.setIdOrgtn(orgId);
        questionnaireDTO.setDtFsclYr(dtfsclYr);
        questionnaireDTO.setIdApplTrkg(idApplTrkg);
        questionnaireDTO.setCdAppl(Const.FORM16);
        questionnaireDTO.setApplFormLink(applFormLink);
        questionnaireDTO.setFormStatus(formStatus);
        questionnaireDTO.setPreviousSection(previousSection);
        questionnaireDTO.setNextSection(nextSection);
        questionnaireDTO.setDttmCreate(dateFormat.format(new Date(ucsApplTrkg.getDttmCreate().getTime())));
        questionnaireDTO.setInstnType(findInstnType(orgId, dtfsclYr, idApplTrkg));

        setComplianceQstnrs(questionnaireDTO, ucsApplTrkgCreateDate);

        InsuranceQuestionnaireDTO insuranceQuestionnaireDTO = new InsuranceQuestionnaireDTO();
        List<QuestionnaireDecisionDTO> insuranceDecisions = getInsuranceDecisions(
                orgId, dtfsclYr, idApplTrkg, insuranceQuestionnaireDTO);
        insuranceQuestionnaireDTO.setInsuranceDecisions(insuranceDecisions);
        questionnaireDTO.setInsuranceQuestionnaireDTO(insuranceQuestionnaireDTO);

        ServicingDTO servicingDTO = getServicing(orgId, dtfsclYr, idApplTrkg, ucsApplTrkgCreateDate);
        questionnaireDTO.setServicing(servicingDTO);

        OriginationsDTO originationsDTO = getOriginations(orgId, dtfsclYr, idApplTrkg, ucsApplTrkgCreateDate);
        questionnaireDTO.setOriginations(originationsDTO);

        AuditedFinancialStatementsDTO auditedFinancialStatements = getAuditedFinancialStatements(
                orgId, dtfsclYr, idApplTrkg, questionnaireDTO);

        questionnaireDTO.setReviewFlagExists(form16ServiceHelper.isFormEleTrkgRecordExists(questionnaireDTO.getIdFormTrkg()));
        questionnaireDTO.setForgnOpr(getForgnOperList(orgId, Const.F16_QUES_INFOSE_Q9, dtfsclYr, idApplTrkg));

        List<UcsApplQstnr> qustnrHelperTxtList = ucsApplQstnrRepository
                .findByCodeLikeAndEffDateRange(Const.QUESTIONS_HELPERTXT_CD, ucsApplTrkgCreateDate);
        List<UcsApplQstnr> ofacHeader = ucsApplQstnrRepository.findByCode(Const.F16_OFAC_HEADER);

        return questionnaireDTO;
    }

    private List<QuestionnaireDecisionDTO> getMIQuestionnaire(Integer orgId,
            Short dtfsclYr, Integer idApplTrkg, Date ucsApplTrkgCreateDate) {
        List<QuestionnaireDecisionDTO> miQuestionaire = new ArrayList<>();
        String dttmCreate = dateFormat.format(ucsApplTrkgCreateDate);
        String primarySsrNumber = ucsCntprtyAcctRepository.findPrimarySSRNumberByOrgId(orgId).orElse(null);
        boolean isSrvcrRoleActive =
                ucsCntprtyAcctRoleAssnRepository.existsByIdIdCntprtyAcctAndUcsOrgtnRole_IdOrgtnRoleAndDtCntprtyAcctRoleExptnsNull(primarySsrNumber, Const.ID_SVCR_CODE);

        if (shouldShowFormSection(isSrvcrRoleActive, dttmCreate, RecertConstants.MI_SECTION_VISIBLE_ONLY_FOR_SVCR_FROM)) {
            List<UcsApplQstnr> miQuestions = ucsApplQstnrRepository
                    .findByCodeLikeAndEffDateRangeAndOrderBy(Const.MI_CANCELLATION_QUESTIONS, ucsApplTrkgCreateDate);

            Map<String, UcsApplQstnr> miQuestionaireMap = new HashMap<>();
            for (UcsApplQstnr miQuestion : miQuestions) {
                miQuestionaireMap.put(miQuestion.getCdQstnr(), miQuestion);
            }

            List<UcsFinlInfoDcsnTran> ucsFinlInfoDcsnTranList = ucsFinlInfoDcsnTransRepository
                    .findByUcsFinlInfoTransPKIdOrgtn(orgId, dtfsclYr, idApplTrkg);

            Map<String, UcsFinlInfoDcsnTran> miQuestionValuePairMap = new HashMap<>();
            for (UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran : ucsFinlInfoDcsnTranList) {
                miQuestionValuePairMap.put(ucsFinlInfoDcsnTran.getId().getCdFinlInfoDcsn(), ucsFinlInfoDcsnTran);
            }

            QuestionnaireDecisionDTO questionnaireDecisionDTO = null;
            for (UcsApplQstnr miQuestion : miQuestions) {
                String cdQstnr = miQuestion.getCdQstnr();
                questionnaireDecisionDTO = new QuestionnaireDecisionDTO();
                questionnaireDecisionDTO.setCdqstnrdscn(cdQstnr);
                questionnaireDecisionDTO.setQstnText(miQuestion.getDescQstnr());

                UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran = miQuestionValuePairMap.get(cdQstnr);
                if (ucsFinlInfoDcsnTran != null) {
                    questionnaireDecisionDTO.setIndDcsn(ucsFinlInfoDcsnTran.getIndFinlInfoDcsn());
                    questionnaireDecisionDTO.setTextDcsn(ucsFinlInfoDcsnTran.getTextFinlInfoDcsn());
                }
                miQuestionaire.add(questionnaireDecisionDTO);
            }
        }

        return miQuestionaire;
    }

    private AuditedFinancialStatementsDTO getAuditedFinancialStatements(
            Integer orgId, Short dtfsclYr, Integer idApplTrkg,
            QuestionnaireDTO questionnaireDTO) {
        AuditedFinancialStatementsDTO auditedFinancialStatements = new AuditedFinancialStatementsDTO();
        List<UcsApplQstnr> auditedFinancialStatementsQuestions = ucsApplQstnrRepository
                .findByCodeLike(Const.AUDITED_FINANCIAL_STATEMENTS_QUESTIONS_CD);
        Map<String, UcsApplQstnr> auditedFinancialStatementsQuestionsMap = new HashMap<>();
        for (UcsApplQstnr auditedFinancialStatementQuestion : auditedFinancialStatementsQuestions) {
            auditedFinancialStatementsQuestionsMap.put(
                    auditedFinancialStatementQuestion.getCdQstnr(),
                    auditedFinancialStatementQuestion);
        }

        List<UcsFinlInfoDcsnTran> ucsFinlInfoDcsnTranList = ucsFinlInfoDcsnTransRepository
                .findByUcsFinlInfoTransPKIdOrgtn(orgId, dtfsclYr, idApplTrkg);

        Map<String, UcsFinlInfoDcsnTran> auditedFinancialStatementsCdValuePairMap = new HashMap<>();
        for (UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran : ucsFinlInfoDcsnTranList) {
            auditedFinancialStatementsCdValuePairMap.put(ucsFinlInfoDcsnTran.getId().getCdFinlInfoDcsn(),
                    ucsFinlInfoDcsnTran);
        }

        List<QuestionnaireDecisionDTO> auditedFinancialStatementsDecisions = new ArrayList<>();
        QuestionnaireDecisionDTO questionnaireDecisionDTO = null;
        for (UcsApplQstnr auditedFinancialStatementQuestion : auditedFinancialStatementsQuestions) {
            String cdQstnr = auditedFinancialStatementQuestion.getCdQstnr();
            if (!Const.F16_QSTNR_FNCLSTATEMNTS_HELPTXT.equals(cdQstnr)) {
                questionnaireDecisionDTO = new QuestionnaireDecisionDTO();
                questionnaireDecisionDTO.setCdqstnrdscn(cdQstnr);
                questionnaireDecisionDTO.setQstnText(auditedFinancialStatementQuestion.getDescQstnr());

                UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran = auditedFinancialStatementsCdValuePairMap.get(cdQstnr);
                if (ucsFinlInfoDcsnTran != null) {
                    questionnaireDecisionDTO.setIndDcsn(ucsFinlInfoDcsnTran.getIndFinlInfoDcsn());
                    questionnaireDecisionDTO.setTextDcsn(ucsFinlInfoDcsnTran.getTextFinlInfoDcsn());
                }
                auditedFinancialStatementsDecisions.add(questionnaireDecisionDTO);
            }
        }

        UcsFinlInfoTranPK ucsFinlInfoTranPK = new UcsFinlInfoTranPK();
        ucsFinlInfoTranPK.setDtFsclYr(dtfsclYr);
        ucsFinlInfoTranPK.setIdApplTrkg(idApplTrkg);
        ucsFinlInfoTranPK.setIdOrgtn(orgId);

        Optional<UcsFinlInfoTran> ucsFinlInfoTranOptional = ucsFinlInfoTransRepository.findById(ucsFinlInfoTranPK);
        if (ucsFinlInfoTranOptional.isPresent()) {
            UcsFinlInfoTran ucsFinlInfoTran = ucsFinlInfoTranOptional.get();
            questionnaireDecisionDTO = new QuestionnaireDecisionDTO();
            questionnaireDecisionDTO.setTextDcsn(ucsFinlInfoTran.getNameFinstmtPrpardLvl());
            questionnaireDecisionDTO.setQstnText(Const.F16_QSTNR_FNCLSTATEMNTS_Q3_HARDCODED);
            questionnaireDecisionDTO.setCdqstnrdscn(Const.F16_QSTNR_FNCLSTATEMNTS_Q3);
            auditedFinancialStatementsDecisions.add(questionnaireDecisionDTO);
            Timestamp dtFinstmtAvail = ucsFinlInfoTran.getDtFinstmtAvail();
            if (dtFinstmtAvail != null) {
                LocalDate date = dtFinstmtAvail.toLocalDateTime().toLocalDate();
                auditedFinancialStatements.setStatementAvailableOn(
                        DateUtil.convertLocalDateToStringByFormat(date, Form16ServiceHelper.DATE_PATTERN));
            }
        }

        auditedFinancialStatements.setAuditedFinancialStatementsDecisions(auditedFinancialStatementsDecisions);

        Map<String, String> externalAuditorNames = new HashMap<>();
        if (questionnaireDTO.getLookup() != null && questionnaireDTO.getLookup().getExternalAuditorNames() != null) {
            for (LookupDTO lookup : questionnaireDTO.getLookup().getExternalAuditorNames()) {
                externalAuditorNames.put(lookup.getTypeCode(), lookup.getLookupData());
            }
        }

        UcsOrgtnExtnlAudtrTrans ucsOrgtnExtnlAudtrTrans = ucsOrgtnExtnlAudtrTransRepository
                .findByIdApplTrkgDtFsclYrIdOrgtn(orgId, idApplTrkg, dtfsclYr);
        LookupDTO externalAuditorName = new LookupDTO();
        if (ucsOrgtnExtnlAudtrTrans != null) {
            String typeCode = ucsOrgtnExtnlAudtrTrans.getId().getIdExtnlAudtr().toString();
            externalAuditorName.setTypeCode(typeCode);
            externalAuditorName.setLookupData(externalAuditorNames.get(typeCode));
            externalAuditorName.setSrtOrd(0);
            auditedFinancialStatements.setExternalAuditorChangeDesc(ucsOrgtnExtnlAudtrTrans.getDescOthrExtnlAudtr());
        }

        auditedFinancialStatements.setExternalAuditorName(externalAuditorName);

        return auditedFinancialStatements;
    }

    private List<QuestionnaireDecisionDTO> getWHLDecisions(Integer orgId, Short dtfsclYr, Integer idApplTrkg) {
        List<QuestionnaireDecisionDTO> whlDecisions = new ArrayList<>();
        List<UcsApplQstnr> whlQuestions = ucsApplQstnrRepository.findByCodeLike(Const.WHL_QUESTIONS_CD);

        Map<String, UcsApplQstnr> whlQuestionsMap = new HashMap<>();
        for (UcsApplQstnr whlQuestion : whlQuestions) {
            whlQuestionsMap.put(whlQuestion.getCdQstnr(), whlQuestion);
        }

        List<UcsFinlInfoDcsnTran> ucsFinlInfoDcsnTranList = ucsFinlInfoDcsnTransRepository
                .findByUcsFinlInfoTransPKIdOrgtn(orgId, dtfsclYr, idApplTrkg);

        Map<String, UcsFinlInfoDcsnTran> whlCdValuePairMap = new HashMap<>();
        for (UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran : ucsFinlInfoDcsnTranList) {
            whlCdValuePairMap.put(ucsFinlInfoDcsnTran.getId().getCdFinlInfoDcsn(), ucsFinlInfoDcsnTran);
        }

        QuestionnaireDecisionDTO questionnaireDecisionDTO = null;
        for (UcsApplQstnr whlQuestion : whlQuestions) {
            String cdQstnr = whlQuestion.getCdQstnr();
            questionnaireDecisionDTO = new QuestionnaireDecisionDTO();
            questionnaireDecisionDTO.setCdqstnrdscn(cdQstnr);
            questionnaireDecisionDTO.setQstnText(whlQuestion.getDescQstnr());

            UcsFinlInfoDcsnTran ucsFinlInfoDcsnTran = whlCdValuePairMap.get(cdQstnr);
            if (ucsFinlInfoDcsnTran != null) {
                questionnaireDecisionDTO.setIndDcsn(ucsFinlInfoDcsnTran.getIndFinlInfoDcsn());
                questionnaireDecisionDTO.setTextDcsn(ucsFinlInfoDcsnTran.getTextFinlInfoDcsn());
            }
            whlDecisions.add(questionnaireDecisionDTO);
        }

        return whlDecisions;
    }

    private QuestionnaireHelperDTO getQstnrHelpTxt(List<UcsApplQstnr> qustnrHelperTxtList, List<UcsApplQstnr> ofacHeader) {
        QuestionnaireHelperDTO questionnaireHelperTxtDTO = new QuestionnaireHelperDTO();
        for (UcsApplQstnr helpertxts : qustnrHelperTxtList) {
            if (Const.F16_QSTNR_ORIGN_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setOrignHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_WAR_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setWareHouseHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_MI_CANCELLATION_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setMiCancellationHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_SVCR_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setSvcrHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_OFAC_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setOfacHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_INFOSE_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setInfosecHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.F16_QSTNR_CHGREP_HELPERTXT.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setChgRepHelperText(helpertxts.getDescQstnr().trim());
            } else if (Const.INSURANCE_QUESTIONS_HELPERTXT_CD.equalsIgnoreCase(helpertxts.getCdQstnr())) {
                questionnaireHelperTxtDTO.setInsuranceHelperText(helpertxts.getDescQstnr().trim());
            }
        }

        if (ofacHeader != null && !ofacHeader.isEmpty()) {
            questionnaireHelperTxtDTO.setOfacHeaderText(ofacHeader.get(0).getDescQstnr());
        }

        return questionnaireHelperTxtDTO;
    }

    private void saveOriginationBusPercent(Integer orgId, Short dtfsclYr,
            Integer idApplTrkg, OriginationBusPercentDTO origBusPercentDto) {
        List<OriginationBusPercentEnum> busPercentEnumList = Arrays.asList(OriginationBusPercentEnum.values());
        List<UcsTpoOwnpTran> ucsTpoOwnpTranListObj = new ArrayList<>();
        saveTPOTrans(orgId, dtfsclYr, idApplTrkg);

        for (OriginationBusPercentEnum enumObj : busPercentEnumList) {
            UcsTpoOwnpTran ucsTpoOwnpTran = new UcsTpoOwnpTran();
            UcsTpoOwnpTranPK ucsTpoOwnpTranPK = new UcsTpoOwnpTranPK();
            ucsTpoOwnpTranPK.setIdOrgtn(orgId);
            ucsTpoOwnpTranPK.setDtFsclYr(dtfsclYr);
            ucsTpoOwnpTranPK.setIdApplTrkg(idApplTrkg);

            if (Const.F16_QSTNR_ORIGN_TPO_WHOLESALE.equals(enumObj.getBusinessCategory())) {
                ucsTpoOwnpTranPK.setCdTpoOwnp(Const.F16_QSTNR_ORIGN_TPO_WHOLESALE);
                ucsTpoOwnpTran.setPctTpoOwnp(origBusPercentDto.getBrkPctval() != null ?
                        BigDecimal.valueOf(origBusPercentDto.getBrkPctval()) : null);
            } else if (Const.F16_QSTNR_ORIGN_TPO_BROKER.equals(enumObj.getBusinessCategory())) {
                ucsTpoOwnpTranPK.setCdTpoOwnp(Const.F16_QSTNR_ORIGN_TPO_BROKER);
                ucsTpoOwnpTran.setPctTpoOwnp(origBusPercentDto.getBrkPctval() != null ?
                        BigDecimal.valueOf(origBusPercentDto.getBrkPctval()) : null);
            } else {
                ucsTpoOwnpTranPK.setCdTpoOwnp(Const.F16_QSTNR_ORIGN_TPO_CORRESPONDENT);
                ucsTpoOwnpTran.setPctTpoOwnp(origBusPercentDto.getCorrPctVal() != null ?
                        BigDecimal.valueOf(origBusPercentDto.getCorrPctVal()) : null);
            }

            ucsTpoOwnpTran.setId(ucsTpoOwnpTranPK);
            ucsTpoOwnpTran.setIdUserCreate(Const.UCOUNT_USER);
            ucsTpoOwnpTran.setIdUserLstUpdt(Const.UCOUNT_USER);
            ucsTpoOwnpTran.setDttmCreate(new Timestamp(System.currentTimeMillis()));
            ucsTpoOwnpTran.setDttmLstUpdt(new Timestamp(System.currentTimeMillis()));
            ucsTpoOwnpTranListObj.add(ucsTpoOwnpTran);
        }
        ucsTpoOwnpTranRepository.saveAll(ucsTpoOwnpTranListObj);
    }

    public void saveTPOTrans(Integer orgId, short year, Integer applTrkgId) {
        UcsTpoTranPK ucsTpoTranPK = new UcsTpoTranPK();
        UcsTpoTran ucsTpoTran = new UcsTpoTran();
        ucsTpoTranPK.setDtFsclYr(year);
        ucsTpoTranPK.setIdApplTrkg(applTrkgId);
        ucsTpoTranPK.setIdOrgtn(orgId);
        ucsTpoTran.setId(ucsTpoTranPK);
        ucsTpoTran.setIdUserCreate(Const.UCOUNT_USER);
        ucsTpoTran.setIdUserLstUpdt(Const.UCOUNT_USER);
        ucsTpoTran.setDttmCreate(new Timestamp(System.currentTimeMillis()));
        ucsTpoTran.setDttmLstUpdt(new Timestamp(System.currentTimeMillis()));
        ucsTpoTranRepository.save(ucsTpoTran);
    }

    private OriginationBusPercentDTO getOriginationBusPercent(Integer orgId, String f16QuesOrgdneCo,
            Short dtfsclYr, Integer idApplTrkg) {
        OriginationBusPercentDTO origBusPercentDto = new OriginationBusPercentDTO();
        List<UcsTpoOwnpTran> busPercent = ucsTpoOwnpTranRepository
                .findByid_IdOrgthnAndId_DtFsclYrAndId_IdApplTrkg(orgId, dtfsclYr, idApplTrkg);

        for (UcsTpoOwnpTran bp : busPercent) {
            if (Const.F16_QSTNR_ORIGN_TPO_WHOLESALE.equals(bp.getId().getCdTpoOwnp())) {
                origBusPercentDto.setBrkPctVal(bp.getPctTpoOwnp() != null ? bp.getPctTpoOwnp().intValue() : null);
            } else if (Const.F16_QSTNR_ORIGN_TPO_BROKER.equals(bp.getId().getCdTpoOwnp())) {
                origBusPercentDto.setBrkPctVal(bp.getPctTpoOwnp() != null ? bp.getPctTpoOwnp().intValue() : null);
            } else {
                origBusPercentDto.setCorrPctVal(bp.getPctTpoOwnp() != null ? bp.getPctTpoOwnp().intValue() : null);
            }
        }
        return origBusPercentDto;
    }

    private void setComplianceQstnrs(QuestionnaireDTO questionnaireDTO, Date ucsApplTrkgCreateDate) {
        List<UcsQstnrDcsnTrans> qstnrDcsnTransList = ucsQstnrDcsnTransRepository
                .findByUcsQstnrDcsnTransPKIdOrgtn(questionnaireDTO.getIdOrgtn(), questionnaireDTO.getDtFsclYr(),
                        questionnaireDTO.getIdApplTrkg());
        List<UcsApplQstnr> qstnrList = ucsApplQstnrRepository
                .findByCodeLikeAndEffDateRangeAndOrderBy(Const.F16_Question_Compliance, ucsApplTrkgCreateDate);

        Map<String, String> qstnrMap = qstnrList.stream()
                .distinct()
                .collect(Collectors.toMap(UcsApplQstnr::getCdQstnr, UcsApplQstnr::getDescQstnr));

        List<QuestionnaireDecisionDTO> complianceQstnrs;
        if (CollectionUtils.isEmpty(qstnrDcsnTransList)) {
            complianceQstnrs = qstnrList.stream()
                    .map(qstnr -> {
                        QuestionnaireDecisionDTO qstnrDcsnDTO = new QuestionnaireDecisionDTO();
                        qstnrDcsnDTO.setCdqstnrdscn(qstnr.getCdQstnr());
                        qstnrDcsnDTO.setQstnText(qstnrMap.get(qstnr.getCdQstnr()));
                        return qstnrDcsnDTO;
                    })
                    .collect(Collectors.toList());
        } else {
            complianceQstnrs = qstnrDcsnTransList.stream()
                    .map(qstnrDcsn -> {
                        QuestionnaireDecisionDTO qstnrDcsnDTO = new QuestionnaireDecisionDTO();
                        qstnrDcsnDTO.setCdqstnrdscn(qstnrDcsn.getId().getCdQstnrDcsn());
                        qstnrDcsnDTO.setQstnText(qstnrMap.get(qstnrDcsn.getId().getCdQstnrDcsn()));
                        qstnrDcsnDTO.setIndDcsn(qstnrDcsn.getIndQstnrDcsn());
                        qstnrDcsnDTO.setTextDcsn(qstnrDcsn.getTextQstnrDcsn());
                        return qstnrDcsnDTO;
                    })
                    .collect(Collectors.toList());
        }

        questionnaireDTO.setComplianceQstnrs(complianceQstnrs);
    }

    private void saveComplianceQstnrs(QuestionnaireDTO idtfnbDtoObj) {
        List<QuestionnaireDecisionDTO> qstnrDcsns = idtfnbDtoObj.getComplianceQstnrs();
        if (CollectionUtils.isEmpty(qstnrDcsns)) {
            return;
        }

        List<UcsQstnrDcsnTrans> qstnrDcsnTransList = qstnrDcsns.stream()
                .map(qstnrDcsn -> {
                    UcsQstnrDcsnTrans ucsQstnrDcsnTrans = new UcsQstnrDcsnTrans();
                    ucsQstnrDcsnTrans.setIdOrgtn(idtfnbDtoObj.getIdOrgtn());
                    ucsQstnrDcsnTrans.setDtFsclYr(idtfnbDtoObj.getDtFsclYr());
                    ucsQstnrDcsnTrans.setIdApplTrkg(idtfnbDtoObj.getIdApplTrkg());
                    ucsQstnrDcsnTrans.setIndQstnrDcsn(qstnrDcsn.getIndDcsn());
                    ucsQstnrDcsnTrans.setTextQstnrDcsn(qstnrDcsn.getTextDcsn());
                    ucsQstnrDcsnTrans.setIdUserCreate(Const.UCOUNT_USER);
                    ucsQstnrDcsnTrans.setIdUserLstUpdt(Const.UCOUNT_USER);
                    ucsQstnrDcsnTrans.setDttmCreate(new Timestamp(System.currentTimeMillis()));
                    ucsQstnrDcsnTrans.setDttmLstUpdt(new Timestamp(System.currentTimeMillis()));
                    return ucsQstnrDcsnTrans;
                })
                .collect(Collectors.toList());

        ucsQstnrDcsnTransRepository.saveAll(qstnrDcsnTransList);
    }

    private List<LookupDTO> getForgnOperList(Integer orgId, String f16QuesInfoseQ9, Short dtfsclYr, Integer idApplTrkg) {
        List<LookupDTO> forgnOprList = new ArrayList<>();
        List<ForgnOperations> oprFmEnum = Arrays.asList(ForgnOperations.values());
        List<UcsintlOperTran> oprFromRepo = ucsintlOperTranRepository
                .findByid_IdOrgtnAndid_DtFsclYrAndid_IdApplTrkg(orgId, dtfsclYr, idApplTrkg);

        for (ForgnOperations fop : oprFmEnum) {
            LookupDTO fopLookupDto = new LookupDTO();
            fopLookupDto.setLookupData(fop.getForgnOpr());
            fopLookupDto.setTypeCode(fop.getCdCntry());
            for (UcsintlOperTran fopTrans : oprFromRepo) {
                if (fopLookupDto.getLookupData().equals(fopTrans.getTextPerfdActvty())) {
                    fopLookupDto.setChecked(true);
                }
            }
            forgnOprList.add(fopLookupDto);
        }

        return forgnOprList;
    }

    public boolean shouldShowFormSection(boolean isSrvcrRoleActive, String dttmCreate, String cutOffDate) {
        try {
            Date dateCutOff = dateFormat.parse(cutOffDate);
            Date dateCreate = dateFormat.parse(dttmCreate);
            if (isSrvcrRoleActive) {
                return true;
            } else {
                return dateCreate.before(dateCutOff);
            }
        } catch (ParseException parseException) {
            throw new UcsApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unparsable date: ", parseException);
        }
    }

    private List<QuestionnaireDecisionDTO> getRoleBasedOfacQuestions(Integer orgId,
            Short dtfsclYr, Integer idApplTrkg, Date ucsApplTrkgCreateDate) {
        return new ArrayList<>();
    }

    private List<QuestionnaireDecisionDTO> getOFACDecisions(Integer orgId,
            Short dtfsclYr, Integer idApplTrkg, Date ucsApplTrkgCreateDate) {
        return new ArrayList<>();
    }

    private List<QuestionnaireDecisionDTO> getInsuranceDecisions(Integer orgId,
            Short dtfsclYr, Integer idApplTrkg, InsuranceQuestionnaireDTO insuranceQuestionnaireDTO) {
        return new ArrayList<>();
    }

    private ServicingDTO getServicing(Integer orgId, Short dtfsclYr, Integer idApplTrkg, Date ucsApplTrkgCreateDate) {
        return new ServicingDTO();
    }

    private OriginationsDTO getOriginations(Integer orgId, Short dtfsclYr, Integer idApplTrkg, Date ucsApplTrkgCreateDate) {
        return new OriginationsDTO();
    }

    private QuestionnaireLookUpWrapperDTO setUpQuestionnaireLookup() {
        return new QuestionnaireLookUpWrapperDTO();
    }

    private String findInstnType(Integer orgId, Short dtfsclYr, Integer idApplTrkg) {
        return null;
    }
}
