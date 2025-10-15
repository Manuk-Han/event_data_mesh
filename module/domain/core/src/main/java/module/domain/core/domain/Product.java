package module.domain.core.domain;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder @Getter @Setter
public class Product {
    private Long id;

    private String name;

    private String description;

    private String fileUrl;

    private String fileKey;
}
