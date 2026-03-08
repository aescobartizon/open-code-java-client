---
name: scaffold-project-structure
description: "Procedimiento paso a paso para crear la estructura de directorios y archivos plantilla de un proyecto microservicios en el momento cero. Solo requiere nombre, descripcion y equipo. No crea servicios, frontends ni infra — eso es responsabilidad de AgentArchitect y AgentDevOps."
license: MIT
compatibility: opencode
---

## Que hace esta skill

Genera mediante comandos bash la estructura base de directorios y archivos plantilla
de un proyecto microservicios, sustituyendo los placeholders por los valores reales.

En este momento cero del proyecto **no se conocen** los microservicios, frontends
ni el proveedor de nube, por lo que:
- `services/` se crea con solo un `.gitkeep`
- `frontend/` se crea con solo un `.gitkeep`
- `infra/` se crea con solo un `.gitkeep`
- `.github/workflows/` se crea con solo un `.gitkeep`

## Variables requeridas

| Variable | Origen |
|---|---|
| `PROYECTO` | `nombre_proyecto` del usuario |
| `BASE_DIR` | `directorio_base` del usuario (o directorio actual) |
| `FECHA` | Fecha actual en formato `YYYY-MM-DD` |

## Procedimiento

### Paso 1 — Verificar que el directorio destino NO existe

```powershell
# Windows PowerShell
Test-Path "{BASE_DIR}\{PROYECTO}"
```
```bash
# Unix/macOS
[ -d "{BASE_DIR}/{PROYECTO}" ] && echo "EXISTE" || echo "LIBRE"
```

Si el directorio EXISTE: alertar al usuario y detener la ejecucion.

---

### Paso 2 — Crear directorios raiz de documentacion

```powershell
$dirs = @(
  ".opencode\agents",
  "docs\arquitectura\diagramas",
  "docs\arquitectura\decisiones",
  "docs\requisitos\negocio",
  "docs\requisitos\funcionales",
  "docs\requisitos\no-funcionales",
  "docs\historias-usuario",
  "docs\api-specs\openapi",
  "docs\api-specs\asyncapi",
  "docs\api-specs\graphql",
  "docs\trazabilidad",
  "docs\refinamientos",
  "docs\informes-ejecutivos",
  "docs\runbooks",
  "tests\e2e",
  "tests\rendimiento",
  "tests\planes-de-prueba",
  "scripts",
  ".github\workflows"
)
foreach ($dir in $dirs) {
  New-Item -ItemType Directory -Force -Path "{BASE_DIR}\{PROYECTO}\$dir" | Out-Null
}
```

---

### Paso 3 — Crear directorios vacios (con .gitkeep) para capas sin definir

```powershell
$placeholders = @(
  "services",
  "frontend",
  "shared",
  "infra",
  "docs\api-specs\openapi",
  "docs\api-specs\asyncapi",
  "docs\api-specs\graphql",
  "docs\arquitectura\diagramas",
  "tests\e2e",
  "tests\rendimiento",
  ".github\workflows"
)
foreach ($dir in $placeholders) {
  $gitkeep = "{BASE_DIR}\{PROYECTO}\$dir\.gitkeep"
  New-Item -ItemType File -Force -Path $gitkeep | Out-Null
}
```

---

### Paso 4 — Generar archivos plantilla

Usar la herramienta `write` de OpenCode para escribir cada archivo.
Sustituir en todos los archivos:
- `{nombre_proyecto}` → valor del parametro
- `{descripcion}` → valor del parametro
- `{equipo}` → valor del parametro
- `{version_inicial}` → valor del parametro
- `{fecha_actual}` → fecha de hoy en YYYY-MM-DD

**Archivos a generar (en orden):**

```
1.  README.md
2.  AGENTS.md
3.  CONTRIBUTING.md
4.  .gitignore
5.  docs/arquitectura/vision-general.md
6.  docs/arquitectura/mapa-servicios.md          (placeholder)
7.  docs/arquitectura/flujo-datos.md             (placeholder)
8.  docs/arquitectura/decisiones/ADR-0000-plantilla.md
9.  docs/requisitos/negocio/BRS-000-plantilla.md
10. docs/requisitos/negocio/indice.md
11. docs/requisitos/funcionales/FRS-000-plantilla.md
12. docs/requisitos/funcionales/indice.md
13. docs/requisitos/no-funcionales/NFR-rendimiento.md
14. docs/requisitos/no-funcionales/NFR-seguridad.md
15. docs/requisitos/no-funcionales/NFR-disponibilidad.md
16. docs/historias-usuario/US-000-plantilla.md
17. docs/historias-usuario/indice.md
18. docs/trazabilidad/RTM.yaml                   (FUENTE DE VERDAD — enlaces: [])
19. docs/trazabilidad/RTM.md
20. docs/trazabilidad/informe-cobertura.md
21. docs/refinamientos/REFINAMIENTO-000-plantilla.md
22. docs/refinamientos/indice.md
23. docs/informes-ejecutivos/INFORME-EJE-000-plantilla.md
24. docs/informes-ejecutivos/indice.md
25. docs/runbooks/despliegue.md                  (placeholder)
26. docs/runbooks/rollback.md                    (placeholder)
27. docs/runbooks/respuesta-incidentes.md        (placeholder)
28. tests/planes-de-prueba/estrategia-pruebas.md
29. tests/planes-de-prueba/plan-pruebas-v1.md
30. scripts/generar-rtm.sh
31. scripts/validar-trazabilidad.sh
```

---

### Paso 5 — Inicializar repositorio git

```bash
git init "{BASE_DIR}/{PROYECTO}"
git -C "{BASE_DIR}/{PROYECTO}" add .
git -C "{BASE_DIR}/{PROYECTO}" commit -m "chore: estructura base del proyecto inicializada por AgentCreateProjectFromScratch"
```

---

### Paso 6 — Mostrar resumen final

```powershell
# Contar directorios y archivos
$dirs  = (Get-ChildItem -Path "{BASE_DIR}\{PROYECTO}" -Recurse -Directory).Count
$files = (Get-ChildItem -Path "{BASE_DIR}\{PROYECTO}" -Recurse -File).Count
Write-Host "Directorios creados: $dirs"
Write-Host "Archivos creados:    $files"
```

Imprimir al usuario:

```
============================================================
  Proyecto {PROYECTO} inicializado correctamente
============================================================

  Directorio raiz : {BASE_DIR}/{PROYECTO}
  Directorios     : XX
  Archivos        : XX

  Proximos pasos recomendados:
  1. Capturar requisitos de negocio con @AgentRequirementsAnalyst
  2. Definir arquitectura de servicios con @AgentArchitect
  3. Crear historias de usuario con @AgentUserStoryWriter
  4. Provisionar infraestructura con @AgentDevOps
  5. Generar casos de prueba con @AgentTestGenerator

  Trazabilidad:
    Fuente de verdad → docs/trazabilidad/RTM.yaml
    Vista humana     → docs/trazabilidad/RTM.md
    Convenciones     → AGENTS.md
============================================================
```

---

## Verificacion post-ejecucion

Confirmar que estos archivos criticos existen antes de reportar exito:

- [ ] `README.md`
- [ ] `AGENTS.md`
- [ ] `.gitignore`
- [ ] `docs/trazabilidad/RTM.yaml`
- [ ] `docs/trazabilidad/RTM.md`
- [ ] `docs/requisitos/negocio/BRS-000-plantilla.md`
- [ ] `docs/requisitos/funcionales/FRS-000-plantilla.md`
- [ ] `docs/historias-usuario/US-000-plantilla.md`
- [ ] `docs/arquitectura/decisiones/ADR-0000-plantilla.md`
- [ ] `tests/planes-de-prueba/estrategia-pruebas.md`
- [ ] `services/.gitkeep`
- [ ] `frontend/.gitkeep`
- [ ] `infra/.gitkeep`

## Lo que esta skill NO hace

- No crea carpetas de microservicios individuales (`services/{nombre}/`) — `AgentArchitect`
- No crea carpetas de frontends (`frontend/{nombre}/`) — `AgentArchitect`
- No genera ficheros de infraestructura (Terraform, K8s) — `AgentDevOps`
- No genera pipelines CI/CD — `AgentDevOps`
- No genera contratos OpenAPI/AsyncAPI — `AgentArchitect`
