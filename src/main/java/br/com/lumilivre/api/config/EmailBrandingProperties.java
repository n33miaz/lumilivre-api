package br.com.lumilivre.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Single, customizable source of truth for the visual identity applied to every
 * outbound email. Bound from {@code app.email.branding.*} so a deployment can
 * re-skin all transactional emails (brand name, palette, logo, footer details)
 * without touching code or templates.
 *
 * <p>Defaults match the LumiLivre brand (purple palette shared with the web
 * client's {@code tailwind.config.js}: {@code #5E195D / #762075 / #9D4D9C}).
 */
@Component
@ConfigurationProperties(prefix = "app.email.branding")
@Data
public class EmailBrandingProperties {

    /** Brand wordmark shown in the header and footer. */
    private String brandName = "LumiLivre";

    /** Short product descriptor rendered under the wordmark in the header. */
    private String tagline = "Sistema de Gestão de Bibliotecas";

    /**
     * Absolute https URL of the header logo. When blank (default), the bundled
     * white logo is embedded inline via a {@code cid:} reference, which renders
     * reliably across clients without external hosting.
     */
    private String logoUrl = "";

    /** Primary brand color (buttons, accents, links). */
    private String primaryColor = "#762075";

    /** Darker shade used as the left edge of the header gradient. */
    private String primaryDark = "#5E195D";

    /** Lighter accent used as the right edge of the header gradient. */
    private String accentColor = "#9D4D9C";

    /** Page background behind the email card. */
    private String pageBackground = "#f1edf5";

    /** Email card / content background. */
    private String surfaceColor = "#ffffff";

    /** Primary body text color. */
    private String textColor = "#2b2333";

    /** Muted text color (footer, captions, helper copy). */
    private String mutedColor = "#857d92";

    /** Hairline / divider color. */
    private String borderColor = "#ece7f1";

    /** Soft background for highlighted info cards (credentials, callouts). */
    private String infoBackground = "#f7f3fa";

    /** Legal/company name shown in the footer copyright line. */
    private String companyName = "LumiLivre";

    /** Optional postal address line shown in the footer (blank = hidden). */
    private String addressLine = "";

    /** Support address surfaced in the footer help line (blank = hidden). */
    private String supportEmail = "";

    public boolean hasExternalLogo() {
        return logoUrl != null && !logoUrl.isBlank();
    }

    public boolean hasAddress() {
        return addressLine != null && !addressLine.isBlank();
    }

    public boolean hasSupportEmail() {
        return supportEmail != null && !supportEmail.isBlank();
    }
}
