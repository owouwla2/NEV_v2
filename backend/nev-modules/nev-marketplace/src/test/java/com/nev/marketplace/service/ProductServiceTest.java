package com.nev.marketplace.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nev.common.core.exception.ServiceException;
import com.nev.common.satoken.utils.LoginHelper;
import com.nev.marketplace.domain.NevMerchantDO;
import com.nev.marketplace.domain.NevProductDO;
import com.nev.marketplace.dto.ProductQueryDTO;
import com.nev.marketplace.dto.ProductSaveDTO;
import com.nev.marketplace.dto.ProductVO;
import com.nev.marketplace.mapper.NevMerchantMapper;
import com.nev.marketplace.mapper.NevProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
@DisplayName("ProductService")
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private NevMerchantMapper merchantMapper;
    @Mock private NevProductMapper productMapper;

    private ProductService service;
    private MockedStatic<LoginHelper> loginHelper;

    @BeforeEach
    void setUp() {
        service = new ProductService(merchantMapper, productMapper, new ObjectMapper());
        loginHelper = Mockito.mockStatic(LoginHelper.class);
    }

    @AfterEach
    void tearDown() {
        loginHelper.close();
    }

    @Test
    @DisplayName("save: 首次上架时自动创建商家档案并序列化图片")
    void saveCreatesMerchantProfileAndProduct() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(2001L);
        when(merchantMapper.selectOne(any())).thenReturn(null);
        doAnswer(inv -> {
            NevMerchantDO merchant = inv.getArgument(0);
            merchant.setId(50L);
            return 1;
        }).when(merchantMapper).insert(any(NevMerchantDO.class));
        doAnswer(inv -> {
            NevProductDO product = inv.getArgument(0);
            product.setId(100L);
            return 1;
        }).when(productMapper).insert(any(NevProductDO.class));

        ProductVO vo = service.save(saveDto(null));

        assertThat(vo.getId()).isEqualTo(100L);
        assertThat(vo.getMerchantId()).isEqualTo(50L);
        assertThat(vo.getMerchantName()).isEqualTo("merchant-2001");
        assertThat(vo.getImages()).containsExactly("https://nev.demo/a.png", "https://nev.demo/b.png");
        assertThat(vo.getStatus()).isEqualTo("ON_SALE");
        verify(merchantMapper).insert(any(NevMerchantDO.class));
        verify(productMapper).insert(any(NevProductDO.class));
    }

    @Test
    @DisplayName("save: 更新其它商家的商品时拒绝")
    void saveRejectsForeignMerchantProduct() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(2001L);
        when(merchantMapper.selectOne(any())).thenReturn(merchant(50L, 2001L));
        NevProductDO existing = product(100L, 60L, "Old");
        when(productMapper.selectById(100L)).thenReturn(existing);

        assertThatThrownBy(() -> service.save(saveDto(100L)))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("listMine: 当前用户无商家档案时返回空列表")
    void listMineReturnsEmptyWithoutMerchantProfile() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(2001L);
        when(merchantMapper.selectOne(any())).thenReturn(null);

        assertThat(service.listMine()).isEmpty();
    }

    @Test
    @DisplayName("listMine: 返回当前商家的商品并反序列化图片")
    void listMineMapsProductsToVo() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(2001L);
        NevMerchantDO merchant = merchant(50L, 2001L);
        when(merchantMapper.selectOne(any())).thenReturn(merchant);
        NevProductDO product = product(100L, 50L, "NEV 85kWh LFP");
        product.setImages("[\"https://nev.demo/pack.png\"]");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        List<ProductVO> rows = service.listMine();

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTitle()).isEqualTo("NEV 85kWh LFP");
        assertThat(rows.get(0).getMerchantName()).isEqualTo("merchant-2001");
        assertThat(rows.get(0).getImages()).containsExactly("https://nev.demo/pack.png");
    }

    @Test
    @DisplayName("publicSearch: 分页查询公开商品，坏图片 JSON 容错为空列表")
    void publicSearchMapsPageAndToleratesBrokenImagesJson() {
        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword("LFP");
        query.setCategory("BATTERY");
        query.setMerchantId(50L);

        NevProductDO product = product(100L, 50L, "NEV LFP");
        product.setImages("not-json");
        Page<NevProductDO> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(product));
        when(productMapper.selectPage(any(), any())).thenReturn(page);
        when(merchantMapper.selectBatchIds(List.of(50L))).thenReturn(List.of(merchant(50L, 2001L)));

        Page<ProductVO> voPage = service.publicSearch(query);

        assertThat(voPage.getTotal()).isEqualTo(1);
        assertThat(voPage.getRecords()).hasSize(1);
        assertThat(voPage.getRecords().get(0).getMerchantName()).isEqualTo("merchant-2001");
        assertThat(voPage.getRecords().get(0).getImages()).isEmpty();
    }

    @Test
    @DisplayName("save: 未登录拒绝")
    void saveRejectsAnonymousUser() {
        loginHelper.when(LoginHelper::getUserId).thenReturn(null);

        assertThatThrownBy(() -> service.save(saveDto(null)))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("未登录");
    }

    private ProductSaveDTO saveDto(Long id) {
        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setId(id);
        dto.setTitle("NEV 85kWh LFP");
        dto.setSubtitle("demo");
        dto.setCategory("BATTERY");
        dto.setPrice(new BigDecimal("29999.00"));
        dto.setStock(8);
        dto.setBatteryId(500L);
        dto.setImages(List.of("https://nev.demo/a.png", "https://nev.demo/b.png"));
        dto.setDescription("pack");
        return dto;
    }

    private NevMerchantDO merchant(Long id, Long userId) {
        NevMerchantDO merchant = new NevMerchantDO();
        merchant.setId(id);
        merchant.setUserId(userId);
        merchant.setMerchantName("merchant-" + userId);
        return merchant;
    }

    private NevProductDO product(Long id, Long merchantId, String title) {
        NevProductDO product = new NevProductDO();
        product.setId(id);
        product.setMerchantId(merchantId);
        product.setCategory("BATTERY");
        product.setTitle(title);
        product.setSubtitle("demo");
        product.setPrice(new BigDecimal("29999.00"));
        product.setStock(8);
        product.setSalesCount(0);
        product.setBatteryId(500L);
        product.setImages("[]");
        product.setDescription("pack");
        product.setStatus("ON_SALE");
        return product;
    }
}
