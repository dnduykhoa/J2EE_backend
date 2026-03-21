package j2ee_backend.nhom05.service;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import j2ee_backend.nhom05.dto.pcbuilder.PcBuildSelection;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderOptionItemDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderOptionsResponseDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderSlotDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderSummaryResponseDto;
import j2ee_backend.nhom05.dto.pcbuilder.PcBuilderWarningDto;
import j2ee_backend.nhom05.model.Category;
import j2ee_backend.nhom05.model.Product;
import j2ee_backend.nhom05.model.ProductSpecification;
import j2ee_backend.nhom05.model.ProductStatus;
import j2ee_backend.nhom05.repository.ICategoryRepository;
import j2ee_backend.nhom05.repository.IProductRepository;
import j2ee_backend.nhom05.repository.IProductSpecificationRepository;

@Service
public class PcBuilderService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private static final List<String> SLOT_ORDER = List.of("cpu", "mainboard", "ram", "gpu", "storage", "psu", "case", "cooling");

    private static final Map<String, String> SLOT_LABELS = Map.of(
            "cpu", "CPU",
            "mainboard", "Mainboard",
            "ram", "RAM",
            "gpu", "GPU",
            "storage", "Storage",
            "psu", "PSU",
            "case", "Case",
            "cooling", "Cooling");

    private static final Map<String, List<String>> SLOT_CATEGORY_KEYWORDS = Map.of(
            "cpu", List.of("cpu", "processor", "vi xu ly", "bo xu ly"),
            "mainboard", List.of("mainboard", "motherboard", "bo mach chu", "main"),
            "ram", List.of("ram", "memory"),
            "gpu", List.of("gpu", "vga", "card do hoa", "graphics card"),
            "storage", List.of("ssd", "hdd", "storage", "o cung", "nvme"),
            "psu", List.of("psu", "power supply", "nguon"),
            "case", List.of("case", "vo may"),
            "cooling", List.of("cooling", "tan nhiet", "quat", "aio"));

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IProductSpecificationRepository productSpecificationRepository;

    public List<PcBuilderSlotDto> getSlots() {
        List<PcBuilderSlotDto> slots = new ArrayList<>();
        for (String slot : SLOT_ORDER) {
            long count = getProductsForSlot(slot).stream().filter(this::isSellable).count();
            slots.add(new PcBuilderSlotDto(slot, SLOT_LABELS.getOrDefault(slot, slot), count));
        }
        return slots;
    }

    public PcBuilderOptionsResponseDto getOptions(String slot, PcBuildSelection selection) {
        String normalizedSlot = normalize(slot);
        validateSlot(normalizedSlot);
        PcBuildSelection safeSelection = selection != null ? selection : new PcBuildSelection();

        List<Product> candidates = getProductsForSlot(normalizedSlot)
                .stream()
                .filter(this::isSellable)
                .collect(Collectors.toList());

        BuildContext context = buildContext(safeSelection, candidates.stream().map(Product::getId).collect(Collectors.toList()));
        Map<String, String> filters = new LinkedHashMap<>();

        if ("mainboard".equals(normalizedSlot)) {
            String cpuSocket = context.getSpecValue(safeSelection.getCpuId(), "socket", "cpu_socket", "socket_cpu", "cpu-socket");
            if (!cpuSocket.isBlank()) {
                filters.put("cpu_socket", cpuSocket);
                candidates = candidates.stream()
                        .filter(product -> normalizedEquals(context.getSpecValue(product.getId(), "socket", "cpu_socket", "socket_cpu", "cpu-socket"), cpuSocket))
                        .collect(Collectors.toList());
            }
        }

        if ("ram".equals(normalizedSlot)) {
            String boardRamType = context.getSpecValue(safeSelection.getMainboardId(), "ram_type", "memory_type", "ddr", "memory", "ram");
            if (!boardRamType.isBlank()) {
                filters.put("ram_type", boardRamType);
                candidates = candidates.stream()
                        .filter(product -> containsNormalized(context.getSpecValue(product.getId(), "ram_type", "memory_type", "ddr", "memory", "ram"), boardRamType))
                        .collect(Collectors.toList());
            }
        }

        if ("psu".equals(normalizedSlot)) {
            int recommended = recommendedPsuWatt(context);
            filters.put("min_watt", String.valueOf(recommended));
            candidates = candidates.stream()
                    .filter(product -> context.getSpecNumber(product.getId(), "watt", "psu_watt", "max_watt") >= recommended)
                    .collect(Collectors.toList());
        }

        if ("case".equals(normalizedSlot)) {
            String boardFormFactor = context.getSpecValue(safeSelection.getMainboardId(), "form_factor", "mb_form_factor", "motherboard_form_factor");
            if (!boardFormFactor.isBlank()) {
                filters.put("mainboard_form_factor", boardFormFactor);
                candidates = candidates.stream()
                        .filter(product -> containsNormalized(
                                context.getSpecValue(product.getId(), "supported_form_factor", "form_factor", "mb_form_factor"),
                                boardFormFactor))
                        .collect(Collectors.toList());
            }
        }

        if ("gpu".equals(normalizedSlot)) {
            double caseMaxGpuLength = context.getSpecNumber(
                    safeSelection.getCaseId(),
                    "max_gpu_length_mm", "gpu_max_length_mm", "max_gpu_length",
                    "max_vga_length_mm", "vga_max_length_mm", "vga_max_length",
                    "chieu_dai_vga_toi_da", "chieu_dai_gpu_toi_da", "chieu_dai_toi_da_vga");
            if (caseMaxGpuLength > 0) {
                filters.put("max_gpu_length_mm", String.valueOf((int) caseMaxGpuLength));
                candidates = candidates.stream()
                        .filter(product -> {
                            double gpuLength = context.getSpecNumber(
                                    product.getId(),
                                    "gpu_length_mm", "length_mm", "gpu_length", "card_length_mm",
                                    "kich_thuoc", "kich_thuoc_gpu", "dimensions", "size", "chieu_dai");
                            return gpuLength <= 0 || gpuLength <= caseMaxGpuLength;
                        })
                        .collect(Collectors.toList());
            }
        }

        List<PcBuilderOptionItemDto> options = candidates.stream()
                .map(product -> toOptionItem(product, context))
                .collect(Collectors.toList());

        return new PcBuilderOptionsResponseDto(
                normalizedSlot,
                estimatePower(context),
                recommendedPsuWatt(context),
                filters,
                options);
    }

    public PcBuilderSummaryResponseDto getSummary(PcBuildSelection selection) {
        PcBuildSelection safeSelection = selection != null ? selection : new PcBuildSelection();
        BuildContext context = buildContext(safeSelection, Collections.emptyList());
        List<Product> selectedProducts = context.getSelectedProductsInOrder(safeSelection);

        BigDecimal totalPrice = selectedProducts.stream()
                .map(Product::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int estimatedPower = estimatePower(context);
        int recommendedPsu = recommendedPsuWatt(context);

        List<PcBuilderWarningDto> warnings = evaluateWarnings(safeSelection, context, recommendedPsu);
        boolean compatible = warnings.stream().noneMatch(w -> "ERROR".equalsIgnoreCase(w.getSeverity()));

        List<PcBuilderOptionItemDto> selectedParts = selectedProducts.stream()
                .map(product -> toOptionItem(product, context))
                .collect(Collectors.toList());

        return new PcBuilderSummaryResponseDto(
                selectedParts,
                totalPrice,
                estimatedPower,
                recommendedPsu,
                compatible,
                warnings);
    }

    private List<PcBuilderWarningDto> evaluateWarnings(PcBuildSelection selection, BuildContext context, int recommendedPsu) {
        List<PcBuilderWarningDto> warnings = new ArrayList<>();

        String cpuSocket = context.getSpecValue(selection.getCpuId(), "socket", "cpu_socket");
        String boardSocket = context.getSpecValue(selection.getMainboardId(), "socket", "cpu_socket");
        if (!cpuSocket.isBlank() && !boardSocket.isBlank() && !normalizedEquals(cpuSocket, boardSocket)) {
            warnings.add(new PcBuilderWarningDto(
                    "ERROR",
                    "CPU_MAINBOARD_SOCKET_MISMATCH",
                    "CPU socket và mainboard socket không tương thích."));
        }

        String boardRamType = context.getSpecValue(selection.getMainboardId(), "ram_type", "memory_type", "ddr");
        String ramType = context.getSpecValue(selection.getRamId(), "ram_type", "memory_type", "ddr");
        if (!boardRamType.isBlank() && !ramType.isBlank() && !containsNormalized(ramType, boardRamType)) {
            warnings.add(new PcBuilderWarningDto(
                    "ERROR",
                    "MAINBOARD_RAM_TYPE_MISMATCH",
                    "RAM type không khớp với mainboard."));
        }

        double psuWatt = context.getSpecNumber(selection.getPsuId(), "watt", "psu_watt", "max_watt");
        if (selection.getPsuId() != null && psuWatt > 0 && psuWatt < recommendedPsu) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "PSU_INSUFFICIENT_POWER",
                    "PSU có thể không đủ công suất. Nên dùng từ " + recommendedPsu + "W trở lên."));
        }

        String boardFormFactor = context.getSpecValue(selection.getMainboardId(), "form_factor", "mb_form_factor", "motherboard_form_factor");
        String caseSupportedFormFactor = context.getSpecValue(selection.getCaseId(), "supported_form_factor", "form_factor", "mb_form_factor");
        if (!boardFormFactor.isBlank() && !caseSupportedFormFactor.isBlank()
                && !containsNormalized(caseSupportedFormFactor, boardFormFactor)) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "CASE_MAINBOARD_FORM_FACTOR_MISMATCH",
                    "Case có thể không hỗ trợ form factor của mainboard."));
        }

        double caseMaxGpuLength = context.getSpecNumber(
            selection.getCaseId(),
            "max_gpu_length_mm", "gpu_max_length_mm", "max_gpu_length",
            "max_vga_length_mm", "vga_max_length_mm", "vga_max_length",
            "chieu_dai_vga_toi_da", "chieu_dai_gpu_toi_da", "chieu_dai_toi_da_vga");
        double gpuLength = context.getSpecNumber(
            selection.getGpuId(),
            "gpu_length_mm", "length_mm", "gpu_length", "card_length_mm",
            "kich_thuoc", "kich_thuoc_gpu", "dimensions", "size", "chieu_dai");
        if (caseMaxGpuLength > 0 && gpuLength > 0 && gpuLength > caseMaxGpuLength) {
            warnings.add(new PcBuilderWarningDto(
                    "WARNING",
                    "CASE_GPU_LENGTH_MISMATCH",
                    "Case không đủ không gian cho chiều dài GPU đã chọn."));
        }

        if (selection.getCpuId() == null || selection.getMainboardId() == null || selection.getRamId() == null
                || selection.getPsuId() == null) {
            warnings.add(new PcBuilderWarningDto(
                    "INFO",
                    "INCOMPLETE_BUILD",
                    "Build chưa đầy đủ, nên chọn đủ CPU/Mainboard/RAM/PSU để đánh giá chính xác."));
        }

        return warnings;
    }

    private PcBuilderOptionItemDto toOptionItem(Product product, BuildContext context) {
        Map<String, String> keySpecs = new LinkedHashMap<>();
        putSpecIfPresent(keySpecs, "socket", context.getSpecValue(product.getId(), "socket", "cpu_socket"));
        putSpecIfPresent(keySpecs, "ram_type", context.getSpecValue(product.getId(), "ram_type", "memory_type", "ddr"));
        putSpecIfPresent(keySpecs, "form_factor", context.getSpecValue(product.getId(), "form_factor", "mb_form_factor", "motherboard_form_factor"));
        double watt = context.getSpecNumber(product.getId(), "watt", "psu_watt", "max_watt", "tdp", "power_draw");
        if (watt > 0) {
            keySpecs.put("watt", String.valueOf((int) watt));
        }

        return new PcBuilderOptionItemDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getBrand() != null ? product.getBrand().getName() : "",
                product.getCategory() != null ? product.getCategory().getName() : "",
                keySpecs);
    }

    private void putSpecIfPresent(Map<String, String> target, String key, String value) {
        if (!value.isBlank()) {
            target.put(key, value);
        }
    }

    private int estimatePower(BuildContext context) {
        double cpuPower = context.getSpecNumber(context.selection.getCpuId(), "tdp", "cpu_tdp", "power_draw");
        double gpuPower = context.getSpecNumber(context.selection.getGpuId(), "tdp", "gpu_tdp", "power_draw");
        double mainboardPower = context.getSpecNumber(context.selection.getMainboardId(), "tdp", "power_draw");
        double ramPower = context.getSpecNumber(context.selection.getRamId(), "tdp", "power_draw");
        double storagePower = context.getSpecNumber(context.selection.getStorageId(), "tdp", "power_draw");
        double coolingPower = context.getSpecNumber(context.selection.getCoolingId(), "tdp", "power_draw");

        double total = cpuPower + gpuPower + mainboardPower + ramPower + storagePower + coolingPower;
        return (int) Math.ceil(total);
    }

    private int recommendedPsuWatt(BuildContext context) {
        int estimated = estimatePower(context);
        if (estimated <= 0) {
            return 0;
        }
        int withHeadroom = (int) Math.ceil(estimated * 1.25);
        return (int) (Math.ceil(withHeadroom / 50.0) * 50);
    }

    private void validateSlot(String slot) {
        if (!SLOT_ORDER.contains(slot)) {
            throw new RuntimeException("Slot không hợp lệ: " + slot);
        }
    }

    private List<Product> getProductsForSlot(String slot) {
        Set<Long> categoryIds = resolveSlotCategoryIds(slot);
        if (categoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findByCategoryIdIn(new ArrayList<>(categoryIds));
    }

    private Set<Long> resolveSlotCategoryIds(String slot) {
        List<String> keywords = SLOT_CATEGORY_KEYWORDS.getOrDefault(slot, List.of());
        if (keywords.isEmpty()) {
            return Collections.emptySet();
        }

        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, List<Category>> childrenByParent = allCategories.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        Set<Long> roots = allCategories.stream()
                .filter(category -> matchesAnyKeyword(category.getName(), keywords))
                .map(Category::getId)
                .collect(Collectors.toSet());

        if (roots.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> result = new java.util.HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (!result.add(current)) {
                continue;
            }
            for (Category child : childrenByParent.getOrDefault(current, List.of())) {
                stack.push(child.getId());
            }
        }
        return result;
    }

    private boolean matchesAnyKeyword(String text, List<String> keywords) {
        String normalizedText = normalize(text);
        return keywords.stream().anyMatch(keyword -> normalizedText.contains(normalize(keyword)));
    }

    private boolean isSellable(Product product) {
        if (product == null) {
            return false;
        }
        ProductStatus status = product.getStatus();
        if (status == ProductStatus.INACTIVE || status == ProductStatus.OUT_OF_STOCK) {
            return false;
        }
        return product.getStockQuantity() != null && product.getStockQuantity() > 0;
    }

    private boolean normalizedEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return normalizeToken(left).equals(normalizeToken(right));
    }

    private boolean containsNormalized(String text, String probe) {
        if (text == null || probe == null) {
            return false;
        }
        String normalizedText = normalizeToken(text);
        String normalizedProbe = normalizeToken(probe);
        return normalizedText.contains(normalizedProbe) || normalizedProbe.contains(normalizedText);
    }

    private String normalizeToken(String value) {
        return normalize(value).replace("-", "").replace("_", "").replace(" ", "");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private class BuildContext {
        private final PcBuildSelection selection;
        private final Map<Long, Product> selectedById;
        private final Map<Long, Map<String, ProductSpecification>> specsByProduct;
        private final Map<Long, List<ProductSpecification>> specListByProduct;

        private BuildContext(PcBuildSelection selection,
                             Map<Long, Product> selectedById,
                             Map<Long, Map<String, ProductSpecification>> specsByProduct,
                             Map<Long, List<ProductSpecification>> specListByProduct) {
            this.selection = selection;
            this.selectedById = selectedById;
            this.specsByProduct = specsByProduct;
            this.specListByProduct = specListByProduct;
        }

        private String getSpecValue(Long productId, String... keys) {
            ProductSpecification spec = findSpec(productId, keys);
            if (spec == null) {
                return "";
            }
            if (spec.getSpecValue() != null && !spec.getSpecValue().isBlank()) {
                return spec.getSpecValue();
            }
            if (spec.getValueNumber() != null) {
                return spec.getValueNumber().stripTrailingZeros().toPlainString();
            }
            return "";
        }

        private double getSpecNumber(Long productId, String... keys) {
            ProductSpecification spec = findSpec(productId, keys);
            if (spec == null) {
                return 0;
            }
            if (spec.getValueNumber() != null) {
                return spec.getValueNumber().doubleValue();
            }
            return parseNumber(spec.getSpecValue());
        }

        private ProductSpecification findSpec(Long productId, String... keys) {
            if (productId == null || keys == null || keys.length == 0) {
                return null;
            }
            Map<String, ProductSpecification> specs = specsByProduct.get(productId);
            if (specs == null || specs.isEmpty()) {
                return null;
            }
            for (String key : keys) {
                ProductSpecification matched = specs.get(normalize(key));
                if (matched != null) {
                    return matched;
                }
            }

            // Fallback: hỗ trợ key dữ liệu không đồng nhất như "CPU Socket", "socket_cpu", ...
            List<ProductSpecification> specList = specListByProduct.getOrDefault(productId, List.of());
            for (String key : keys) {
                String probe = normalizeToken(key);
                if (probe.isBlank()) {
                    continue;
                }
                for (ProductSpecification spec : specList) {
                    String specKey = normalizeToken(resolveSpecKey(spec));
                    if (specKey.isBlank()) {
                        continue;
                    }
                    if (specKey.equals(probe) || specKey.contains(probe)) {
                        return spec;
                    }
                }
            }
            return null;
        }

        private List<Product> getSelectedProductsInOrder(PcBuildSelection selection) {
            List<Product> products = new ArrayList<>();
            for (Long id : selectedIds(selection)) {
                Product product = selectedById.get(id);
                if (product != null) {
                    products.add(product);
                }
            }
            return products;
        }
    }

    private BuildContext buildContext(PcBuildSelection selection, List<Long> extraProductIds) {
        PcBuildSelection safeSelection = selection != null ? selection : new PcBuildSelection();
        List<Long> selectedIds = selectedIds(safeSelection);
        List<Long> allIds = new ArrayList<>(selectedIds);
        if (extraProductIds != null && !extraProductIds.isEmpty()) {
            allIds.addAll(extraProductIds);
            allIds = allIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        }

        Map<Long, Product> selectedById = selectedIds.isEmpty()
                ? Collections.emptyMap()
                : productRepository.findAllById(selectedIds).stream().collect(Collectors.toMap(Product::getId, product -> product));

        List<ProductSpecification> specs = allIds.isEmpty()
                ? Collections.emptyList()
                : productSpecificationRepository.findByProductIdInOrderByDisplayOrderAsc(allIds);

        Map<Long, Map<String, ProductSpecification>> specsByProduct = new HashMap<>();
        Map<Long, List<ProductSpecification>> specListByProduct = new HashMap<>();
        for (ProductSpecification spec : specs) {
            if (spec.getProduct() == null || spec.getProduct().getId() == null) {
                continue;
            }
            Long productId = spec.getProduct().getId();
            specListByProduct.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(spec);

            String key = normalize(resolveSpecKey(spec));
            if (key.isBlank()) {
                continue;
            }
            specsByProduct
                    .computeIfAbsent(productId, ignored -> new HashMap<>())
                    .putIfAbsent(key, spec);
        }

        return new BuildContext(safeSelection, selectedById, specsByProduct, specListByProduct);
    }

    private String resolveSpecKey(ProductSpecification spec) {
        if (spec.getAttributeDefinition() != null && spec.getAttributeDefinition().getAttrKey() != null) {
            return spec.getAttributeDefinition().getAttrKey();
        }
        return spec.getSpecKey() != null ? spec.getSpecKey() : "";
    }

    private List<Long> selectedIds(PcBuildSelection selection) {
        return Arrays.asList(
                selection.getCpuId(),
                selection.getMainboardId(),
                selection.getRamId(),
                selection.getGpuId(),
                selection.getStorageId(),
                selection.getPsuId(),
                selection.getCaseId(),
                selection.getCoolingId()).stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private double parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(raw.replace(',', '.'));
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
