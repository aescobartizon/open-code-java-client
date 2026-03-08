# {{FRS-ID}} — {{TITLE}}

---

## Metadatos

| Campo                  | Valor                                      |
|------------------------|--------------------------------------------|
| **ID**                 | {{FRS-ID}}                                 |
| **Versión**            | {{VERSION}}                                |
| **Estado**             | {{STATUS}}                                 |
| **Estabilidad**        | {{STABILITY}}                              |
| **Fecha**              | {{DATE}}                                   |
| **Autor**              | {{AUTHOR}}                                 |
| **Revisado por**       | {{REVIEWER}}                               |
| **Aprobado por**       | {{APPROVER}}                               |
| **Épica**              | {{EPIC-ID}} — {{EPIC-TITLE}}               |
| **Prioridad**          | {{PRIORITY}}                               |
| **Fuente**             | {{SOURCE}}                                 |
| **Sesión refinamiento**| {{REF-ID}}                                 |
| **Enlace RTM**         | {{LINK-NNN}}                               |
| **Método verificación**| {{VERIFICATION_METHOD}}                    |

> **Estados válidos:** `borrador` | `en-revisión` | `aprobado` | `rechazado` | `obsoleto`
>
> **Estabilidad válida:** `estable` | `volátil` | `en-cambio`
>
> **Prioridades válidas:** `crítica` | `alta` | `media` | `baja`
>
> **Métodos de verificación:** `test` | `inspección` | `demostración` | `análisis`

---

## Historial de versiones

| Versión | Fecha        | Autor        | Cambios                        |
|---------|--------------|--------------|--------------------------------|
| 1.0.0   | {{DATE}}     | {{AUTHOR}}   | Versión inicial                |

---

## 1. Descripción

> Descripción funcional completa del requisito. Qué debe hacer el sistema, no cómo lo hace.

{{DESCRIPTION}}

---

## 2. Actores y stakeholders

> Distinguir entre **actores del sistema** (interactúan directamente con el sistema) y **stakeholders** (tienen interés en el resultado pero no interactúan directamente).

### 2.1 Actores del sistema

| Actor | Tipo          | Rol en este requisito        |
|-------|---------------|------------------------------|
| {{ACTOR_1}} | `primario` \| `secundario` | {{ACTOR_ROLE_1}} |

> **Tipos:** `primario` (inicia el flujo) | `secundario` (participa en el flujo) | `sistema-externo`

### 2.2 Stakeholders interesados

| Stakeholder | Interés / Impacto                    |
|-------------|--------------------------------------|
| {{STAKEHOLDER_1}} | {{STAKEHOLDER_INTEREST_1}}   |

---

## 3. Entradas y salidas del sistema

> Descripción explícita de los datos que el sistema recibe y produce como resultado de este requisito (IEEE 830 §3.3).

### 3.1 Entradas

| # | Nombre        | Tipo / Formato   | Origen          | Obligatorio | Validaciones                  |
|---|---------------|------------------|-----------------|-------------|-------------------------------|
| 1 | {{INPUT_NAME_1}} | {{INPUT_TYPE_1}} | {{INPUT_SOURCE_1}} | Sí / No | {{INPUT_VALIDATION_1}}     |

### 3.2 Salidas

| # | Nombre         | Tipo / Formato    | Destino          | Descripción                   |
|---|----------------|-------------------|------------------|-------------------------------|
| 1 | {{OUTPUT_NAME_1}} | {{OUTPUT_TYPE_1}} | {{OUTPUT_DEST_1}} | {{OUTPUT_DESC_1}}           |

---

## 4. Precondiciones

> Condiciones que deben cumplirse **antes** de que este requisito pueda ejecutarse.

- {{PRECONDITION_1}}

---

## 5. Postcondiciones

> Estado del sistema **después** de que este requisito se haya ejecutado correctamente.

- {{POSTCONDITION_1}}

---

## 6. Supuestos

> Condiciones que se asumen como verdaderas para que este requisito sea válido. Si un supuesto no se cumple, el requisito debe revisarse (BABOK §2.3.5).

- {{ASSUMPTION_1}}

---

## 7. Flujo principal

> Secuencia de pasos del camino feliz (happy path).

1. {{STEP_1}}
2. {{STEP_2}}
3. {{STEP_3}}

---

## 8. Flujos alternativos

> Variaciones válidas del flujo principal.

### 8.1 {{ALT_FLOW_TITLE}}

- **Condición de activación:** {{ALT_CONDITION}}
- **Pasos:**
  1. {{ALT_STEP_1}}

---

## 9. Flujos de excepción

> Situaciones de error o fallo que el sistema debe manejar.

### 9.1 {{EXC_FLOW_TITLE}}

- **Condición de activación:** {{EXC_CONDITION}}
- **Respuesta del sistema:** {{EXC_RESPONSE}}
- **Código de error (si aplica):** {{EXC_CODE}}

---

## 10. Reglas de negocio

> Restricciones, políticas o lógica de dominio que aplican a este requisito.

| ID     | Regla                         | Fuente / Referencia  |
|--------|-------------------------------|----------------------|
| BR-001 | {{RULE_1}}                    | {{RULE_SOURCE_1}}    |

---

## 11. Requisitos no funcionales asociados

> Requisitos de calidad, rendimiento o seguridad directamente vinculados a este requisito funcional.

| ID         | Tipo           | Descripción              | Criterio medible             |
|------------|----------------|--------------------------|------------------------------|
| {{NFR-ID}} | {{NFR_TYPE}}   | {{NFR_DESCRIPTION}}      | {{NFR_METRIC}}               |

> **Tipos válidos:** `rendimiento` | `seguridad` | `disponibilidad` | `usabilidad` | `escalabilidad` | `mantenibilidad`

---

## 12. Criterios de aceptación

> Condiciones concretas y verificables que deben cumplirse para considerar este requisito implementado y aceptado. Seguir el patrón **Dado / Cuando / Entonces**.

### AC-001 — {{AC_TITLE_1}}

- **Dado** que {{GIVEN_1}}
- **Cuando** {{WHEN_1}}
- **Entonces** {{THEN_1}}

### AC-002 — {{AC_TITLE_2}}

- **Dado** que {{GIVEN_2}}
- **Cuando** {{WHEN_2}}
- **Entonces** {{THEN_2}}

---

## 13. Tests de alto nivel

> Casos de prueba de alto nivel derivados de los criterios de aceptación. No son tests unitarios — cubren el comportamiento observable del sistema desde fuera.

| ID     | Tipo      | Título           | AC vinculado | Precondición del test   | Datos de entrada       | Resultado esperado  | Resultado ejecución | Prioridad |
|--------|-----------|------------------|--------------|-------------------------|------------------------|---------------------|---------------------|-----------|
| TC-001 | funcional | {{TC_TITLE_1}}   | AC-001       | {{TC_PRE_1}}            | {{TC_INPUT_1}}         | {{TC_EXPECTED_1}}   | pendiente           | alta      |
| TC-002 | funcional | {{TC_TITLE_2}}   | AC-002       | {{TC_PRE_2}}            | {{TC_INPUT_2}}         | {{TC_EXPECTED_2}}   | pendiente           | media     |
| TC-003 | borde     | {{TC_TITLE_3}}   | AC-001       | {{TC_PRE_3}}            | {{TC_INPUT_3}}         | {{TC_EXPECTED_3}}   | pendiente           | media     |
| TC-004 | negativo  | {{TC_TITLE_4}}   | AC-002       | {{TC_PRE_4}}            | {{TC_INPUT_4}}         | {{TC_EXPECTED_4}}   | pendiente           | alta      |

> **Tipos de test:** `funcional` | `negativo` | `borde` | `rendimiento` | `seguridad` | `regresión`
>
> **Resultado ejecución:** `pendiente` | `pass` | `fail` | `bloqueado` | `no-aplica`

### Notas de testing

- **Entorno requerido:** {{TEST_ENV}}
- **Datos de prueba:** {{TEST_DATA}}
- **Dependencias externas:** {{TEST_DEPS}}
- **Responsable de ejecución:** {{TEST_OWNER}}

---

## 14. Trazabilidad

| Artefacto             | ID / Referencia  | Descripción              |
|-----------------------|------------------|--------------------------|
| Requisito de negocio  | {{BRS-ID}}       | {{BRS_TITLE}}            |
| Épica                 | {{EPIC-ID}}      | {{EPIC_TITLE}}           |
| Caso de uso           | {{UC-ID}}        | {{UC_TITLE}}             |
| Historia de usuario   | {{US-ID}}        | {{US_TITLE}}             |
| Servicio              | {{SVC-ID}}       | {{SVC_NAME}}             |
| ADR relacionado       | {{ADR-ID}}       | {{ADR_TITLE}}            |
| Sesión refinamiento   | {{REF-ID}}       | {{REF_TITLE}}            |
| Tests de alto nivel   | TC-001, TC-002, TC-003, TC-004 | Ver §13   |
| Enlace RTM            | {{LINK-NNN}}     | Entrada en `traceability/RTM.yaml` |

---

## 15. Dependencias

> Otros requisitos, servicios o sistemas de los que depende este requisito, incluyendo relaciones lógicas entre requisitos.

| Tipo           | ID / Sistema     | Relación                                         | Descripción                   |
|----------------|------------------|--------------------------------------------------|-------------------------------|
| Requisito FRS  | {{DEP_FRS_ID}}   | `depende-de` \| `extiende` \| `reemplaza` \| `conflicto` | {{DEP_DESCRIPTION}}  |
| Servicio ext.  | {{DEP_SVC}}      | `consume` \| `publica`                           | {{DEP_SVC_DESCRIPTION}}       |

> **Relaciones entre requisitos:** `depende-de` | `extiende` | `especializa` | `reemplaza` | `conflicto`

---

## 16. Notas y decisiones abiertas

| # | Nota / Pregunta abierta   | Responsable  | Estado    |
|---|---------------------------|--------------|-----------|
| 1 | {{NOTE_1}}                | {{OWNER_1}}  | abierta   |

---

*Plantilla: `functional-requirement.template.md` v2.0.0 — AgentProjectFromScratch v1.2.2*
