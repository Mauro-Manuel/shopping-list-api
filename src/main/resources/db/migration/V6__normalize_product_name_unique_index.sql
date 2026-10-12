-- RN-PRO-04: Unicidade do nome do produto por utilizador,
-- ignorando maiúsculas/minúsculas e espaços repetidos.

DROP INDEX IF EXISTS uk_products_user_name;

CREATE UNIQUE INDEX uk_products_user_name
    ON products (
                 user_id,
                 LOWER(
        REGEXP_REPLACE(
            BTRIM(name),
            '[[:space:]]+',
            ' ',
            'g'
        )
    )
        );