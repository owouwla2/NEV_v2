package com.nev.marketplace.dto;

import com.nev.battery.dto.BatteryRegisterDTO;
import com.nev.battery.dto.ReceiveDTO;
import com.nev.battery.dto.SellDTO;
import com.nev.battery.dto.TransferInDTO;
import com.nev.common.core.validate.encoding.NoReplacementChar;
import com.nev.common.core.validate.encoding.NoReplacementCharValidator;
import com.nev.marketplace.controller.ConsumerOrderController;
import com.nev.marketplace.controller.ConsumerTradeInController;
import com.nev.marketplace.controller.MerchantOrderController;
import com.nev.marketplace.controller.PublicProductController;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
@DisplayName("M-3 输入层编码防御")
class InputEncodingValidationTest {

    private final NoReplacementCharValidator validator = new NoReplacementCharValidator();

    @Test
    @DisplayName("NoReplacementChar 拒绝 U+FFFD（UTF-8 字节 EFBFBD 解码结果）")
    void validatorRejectsReplacementCharacter() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("宁德时代", null)).isTrue();
        assertThat(validator.isValid("损坏字符: \uFFFD", null)).isFalse();
    }

    @Test
    @DisplayName("电池生命周期写入 DTO 的文本字段带 NoReplacementChar")
    void batteryWriteDtosAreAnnotated() throws Exception {
        assertAnnotated(BatteryRegisterDTO.class, "traceNumber", "model", "serialNo",
            "cellSupplier", "cellType", "bmsInfo");
        assertAnnotated(TransferInDTO.class, "traceNumber", "remark");
        assertAnnotated(SellDTO.class, "traceNumber", "orderNo", "remark");
        assertAnnotated(ReceiveDTO.class, "traceNumber", "remark");
    }

    @Test
    @DisplayName("商城写入/查询 DTO 的文本字段带 NoReplacementChar")
    void marketplaceWriteDtosAreAnnotated() throws Exception {
        assertAnnotated(AddressDTO.class, "recipient", "phone", "province", "city", "district", "detail");
        assertAnnotated(OrderCreateDTO.class, "remark");
        assertAnnotated(OrderQueryDTO.class, "status");
        assertAnnotated(ProductQueryDTO.class, "category", "keyword");
        assertAnnotated(ProductSaveDTO.class, "title", "subtitle", "category", "description", "status");
        assertAnnotated(TradeInEvaluateDTO.class, "summary");
        assertAnnotated(TradeInSubmitDTO.class, "oldBatteryTraceNumber", "remark");
    }

    @Test
    @DisplayName("ProductSaveDTO.images 对每个 URL 元素做 NoReplacementChar 校验")
    void productImagesElementTypeIsAnnotated() throws Exception {
        Field images = ProductSaveDTO.class.getDeclaredField("images");
        AnnotatedParameterizedType type = (AnnotatedParameterizedType) images.getAnnotatedType();

        assertThat(type.getAnnotatedActualTypeArguments()[0]
            .isAnnotationPresent(NoReplacementChar.class)).isTrue();
    }

    @Test
    @DisplayName("查询 DTO 和 reason 参数在控制器输入层触发编码校验")
    void controllerInputsTriggerEncodingValidation() throws Exception {
        assertModelAttributeValidated(ConsumerOrderController.class, "list", OrderQueryDTO.class);
        assertModelAttributeValidated(MerchantOrderController.class, "list", OrderQueryDTO.class);
        assertModelAttributeValidated(PublicProductController.class, "list", ProductQueryDTO.class);

        assertThat(ConsumerOrderController.class.isAnnotationPresent(Validated.class)).isTrue();
        assertThat(ConsumerTradeInController.class.isAnnotationPresent(Validated.class)).isTrue();
        assertReasonParamAnnotated(ConsumerOrderController.class, "cancel");
        assertReasonParamAnnotated(ConsumerTradeInController.class, "reject");
    }

    private void assertAnnotated(Class<?> type, String... fields) throws Exception {
        for (String fieldName : fields) {
            assertThat(type.getDeclaredField(fieldName).isAnnotationPresent(NoReplacementChar.class))
                .as("%s.%s", type.getSimpleName(), fieldName)
                .isTrue();
        }
    }

    private void assertModelAttributeValidated(Class<?> controller, String methodName, Class<?> dtoType) throws Exception {
        Method method = controller.getDeclaredMethod(methodName, dtoType);
        Parameter parameter = method.getParameters()[0];
        assertThat(parameter.isAnnotationPresent(Valid.class)).isTrue();
        assertThat(parameter.isAnnotationPresent(ModelAttribute.class)).isTrue();
    }

    private void assertReasonParamAnnotated(Class<?> controller, String methodName) throws Exception {
        Method method = controller.getDeclaredMethod(methodName, Long.class, String.class);
        Parameter reason = method.getParameters()[1];
        assertThat(reason.isAnnotationPresent(RequestParam.class)).isTrue();
        assertThat(reason.isAnnotationPresent(NoReplacementChar.class)).isTrue();
    }
}
