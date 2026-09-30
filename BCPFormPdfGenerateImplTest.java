package com.test.ucount.recertapi.pdf;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BCPFormPdfGenerateImplTest {

    @InjectMocks
    private BCPFormPdfGenerateImpl bcpFormPdfGenerateImpl;

    @Mock
    private InternalReviewService internalRvwSvc;

    @Mock
    private FileServiceImpl fileService;
}
