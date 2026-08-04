package br.com.lumilivre.api.service.infra.email;

import java.time.Year;
import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.lumilivre.api.config.EmailBrandingProperties;
import br.com.lumilivre.api.config.MessageResolver;

/**
 * Renders the single, unified visual shell shared by every outbound email
 * (header with logo + brand identity, content card, branded footer) and exposes
 * a small set of reusable, inline-styled content components (heading, paragraph,
 * info card, bulletproof button, callout).
 *
 * <p>The markup is intentionally table-based with fully inlined CSS so it renders
 * consistently across Gmail, Outlook, Apple Mail and mobile clients. Brand colors
 * and assets come from {@link EmailBrandingProperties}, making the header
 * customizable per deployment; user-facing copy is resolved per {@link Locale}
 * via {@link MessageResolver}, so the shell honors the same i18n contract as the
 * email body.
 */
@Component
public class EmailTemplate {

    /** Content-ID used when the bundled logo is embedded inline (no external URL). */
    public static final String LOGO_CID = "lumilivre-logo";

    private static final String FONT_STACK =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private final EmailBrandingProperties brand;
    private final MessageResolver messages;

    public EmailTemplate(EmailBrandingProperties brand, MessageResolver messages) {
        this.brand = brand;
        this.messages = messages;
    }

    /** {@code src} attribute for the header logo: external URL or inline CID. */
    public String logoSrc() {
        return brand.hasExternalLogo() ? brand.getLogoUrl() : "cid:" + LOGO_CID;
    }

    public boolean usesInlineLogo() {
        return !brand.hasExternalLogo();
    }

    // ============================ SHELL ============================

    /**
     * Wraps pre-composed inner content in the full branded HTML document.
     *
     * @param innerHtml content built from the component helpers below
     * @param preheader short inbox preview text (plain)
     * @param locale    resolved recipient locale
     */
    public String render(String innerHtml, String preheader, Locale locale) {
        String brandName = escape(brand.getBrandName());
        String tagline = shellTagline(locale);
        String automated = messages.resolve("email.shell.automated-note", locale);
        String rights = messages.resolve("email.shell.rights", locale);
        String footerHelp = messages.resolve("email.shell.support", locale, safe(brand.getSupportEmail()));
        String year = String.valueOf(Year.now().getValue());

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<!DOCTYPE html><html lang=\"").append(locale.toLanguageTag()).append("\" ")
          .append("xmlns=\"http://www.w3.org/1999/xhtml\"><head>")
          .append("<meta charset=\"utf-8\"/>")
          .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>")
          .append("<meta name=\"x-apple-disable-message-reformatting\"/>")
          .append("<meta name=\"color-scheme\" content=\"light dark\"/>")
          .append("<meta name=\"supported-color-schemes\" content=\"light dark\"/>")
          .append("<title>").append(brandName).append("</title>")
          .append("<style>")
          .append("a{text-decoration:none;}")
          .append("body{margin:0;padding:0;width:100%;}")
          .append(".ll-card{box-shadow:0 8px 28px rgba(40,12,46,.10);}")
          .append("@media only screen and (max-width:620px){")
          .append(".ll-wrap{width:100%!important;}")
          .append(".ll-pad{padding-left:22px!important;padding-right:22px!important;}")
          .append(".ll-head{padding:24px 22px!important;}")
          .append("}")
          // Tema escuro: reage à preferência do cliente/SO do usuário, espelhando
          // o comportamento "system" do site. !important vence os estilos inline
          // (que permanecem como fallback claro para clientes sem suporte).
          .append("@media (prefers-color-scheme:dark){")
          .append("body,.ll-page{background:#15171f!important;}")
          .append(".ll-card,.ll-surface{background:#20242e!important;}")
          .append(".ll-text{color:#e8eaed!important;}")
          .append(".ll-muted{color:#9aa3b2!important;}")
          .append(".ll-info{background:rgba(255,255,255,.05)!important;border-color:rgba(255,255,255,.09)!important;}")
          .append(".ll-callout{background:rgba(255,255,255,.05)!important;}")
          .append(".ll-border{border-color:rgba(255,255,255,.09)!important;}")
          .append(".ll-code{color:#d98fd6!important;}")
          .append("}")
          .append("</style></head>");

        sb.append("<body class=\"ll-page\" style=\"margin:0;padding:0;background:").append(brand.getPageBackground())
          .append(";\">");

        // Hidden preheader (inbox preview text) + spacer to prevent body bleed-through.
        sb.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;\">")
          .append(escape(preheader))
          .append("&#8204;&nbsp;&#8204;&nbsp;&#8204;&nbsp;&#8204;&nbsp;&#8204;&nbsp;&#8204;&nbsp;&#8204;&nbsp;</div>");

        // Outer page table.
        sb.append("<table role=\"presentation\" class=\"ll-page\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
          .append("style=\"background:").append(brand.getPageBackground()).append(";\"><tr><td align=\"center\" style=\"padding:28px 14px;\">");

        // Centered card.
        sb.append("<table role=\"presentation\" class=\"ll-wrap ll-card\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
          .append("style=\"width:600px;max-width:600px;background:").append(brand.getSurfaceColor())
          .append(";border-radius:16px;overflow:hidden;\">");

        // ---- Header band (brand gradient + logo + wordmark + tagline) ----
        sb.append("<tr><td class=\"ll-head\" style=\"padding:30px 32px;background:").append(brand.getPrimaryDark())
          .append(";background:linear-gradient(135deg,").append(brand.getPrimaryDark()).append(" 0%,")
          .append(brand.getPrimaryColor()).append(" 52%,").append(brand.getAccentColor()).append(" 100%);text-align:center;\">")
          .append("<table role=\"presentation\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 auto;\"><tr>")
          .append("<td style=\"vertical-align:middle;\">")
          .append("<img src=\"").append(logoSrc()).append("\" width=\"38\" height=\"38\" alt=\"").append(brandName)
          .append("\" style=\"display:block;border:0;outline:none;width:38px;height:38px;\"/>")
          .append("</td><td style=\"vertical-align:middle;padding-left:13px;\">")
          .append("<div style=\"font-family:").append(FONT_STACK)
          .append(";font-size:20px;font-weight:800;color:#ffffff;letter-spacing:.2px;line-height:1.1;\">")
          .append(brandName).append("</div>")
          .append("<div style=\"font-family:").append(FONT_STACK)
          .append(";font-size:12px;font-weight:400;color:rgba(255,255,255,.85);margin-top:3px;line-height:1.2;\">")
          .append(escape(tagline)).append("</div>")
          .append("</td></tr></table></td></tr>");

        // ---- Content (centralizado horizontalmente) ----
        sb.append("<tr><td class=\"ll-pad\" style=\"padding:34px 36px 12px;text-align:center;\">")
          .append(innerHtml)
          .append("</td></tr>");

        // ---- Footer (centralizado) ----
        sb.append("<tr><td class=\"ll-pad ll-surface ll-border\" style=\"padding:24px 36px 30px;text-align:center;background:").append(brand.getSurfaceColor())
          .append(";border-top:1px solid ").append(brand.getBorderColor()).append(";\">")
          .append("<div class=\"ll-text\" style=\"font-family:").append(FONT_STACK).append(";font-size:14px;font-weight:700;color:")
          .append(brand.getTextColor()).append(";\">").append(brandName).append("</div>")
          .append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK).append(";font-size:12px;color:")
          .append(brand.getMutedColor()).append(";margin-top:3px;line-height:1.5;\">").append(escape(tagline)).append("</div>");

        if (brand.hasSupportEmail()) {
            sb.append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK).append(";font-size:12px;color:")
              .append(brand.getMutedColor()).append(";margin-top:12px;line-height:1.5;\">").append(footerHelp).append("</div>");
        }
        if (brand.hasAddress()) {
            sb.append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK).append(";font-size:12px;color:")
              .append(brand.getMutedColor()).append(";margin-top:4px;line-height:1.5;\">").append(escape(brand.getAddressLine())).append("</div>");
        }
        sb.append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK).append(";font-size:11px;color:")
          .append(brand.getMutedColor()).append(";margin-top:16px;line-height:1.5;\">").append(escape(automated)).append("</div>")
          .append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK).append(";font-size:11px;color:")
          .append(brand.getMutedColor()).append(";margin-top:6px;\">&copy; ").append(year).append(" ")
          .append(escape(brand.getCompanyName())).append(". ").append(escape(rights)).append("</div>")
          .append("</td></tr>");

        sb.append("</table>"); // card
        sb.append("</td></tr></table>"); // page
        sb.append("</body></html>");
        return sb.toString();
    }

    private String shellTagline(Locale locale) {
        String fromBundle = messages.resolve("email.shell.tagline", locale);
        return "email.shell.tagline".equals(fromBundle) ? brand.getTagline() : fromBundle;
    }

    // ========================= COMPONENTS =========================

    /** Section title (h1) for the content area, centralizado horizontalmente. */
    public String heading(String text) {
        return "<h1 class=\"ll-text\" style=\"margin:0 0 16px;font-family:" + FONT_STACK
                + ";font-size:22px;line-height:1.3;font-weight:800;text-align:center;color:" + brand.getTextColor() + ";\">"
                + text + "</h1>";
    }

    /** Body paragraph (centralizado). Accepts inline HTML; caller escapes data. */
    public String paragraph(String html) {
        return "<p class=\"ll-text\" style=\"margin:0 0 15px;font-family:" + FONT_STACK
                + ";font-size:15px;line-height:1.65;text-align:center;color:" + brand.getTextColor() + ";\">" + html + "</p>";
    }

    /**
     * Highlighted card listing label/value pairs (e.g. credentials).
     *
     * @param pairs alternating {@code label, value, label, value, ...}
     */
    public String infoCard(String... pairs) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String label = escape(pairs[i]);
            String value = pairs[i + 1]; // may already contain markup
            rows.append("<tr><td style=\"padding:").append(i == 0 ? "0" : "14px").append(" 0 0;text-align:center;\">")
                .append("<div class=\"ll-muted\" style=\"font-family:").append(FONT_STACK)
                .append(";font-size:11px;font-weight:700;letter-spacing:.6px;text-transform:uppercase;color:")
                .append(brand.getMutedColor()).append(";\">").append(label).append("</div>")
                .append("<div class=\"ll-text\" style=\"font-family:").append(FONT_STACK)
                .append(";font-size:16px;font-weight:600;color:").append(brand.getTextColor())
                .append(";margin-top:4px;word-break:break-word;\">").append(value).append("</div>")
                .append("</td></tr>");
        }
        return "<table role=\"presentation\" class=\"ll-info\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"margin:20px 0;background:" + brand.getInfoBackground() + ";border:1px solid "
                + brand.getBorderColor() + ";border-radius:12px;\"><tr><td style=\"padding:20px 22px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">"
                + rows + "</table></td></tr></table>";
    }

    /** Renders a value as a prominent monospace token (codes, passwords). */
    public String code(String value) {
        return "<span class=\"ll-code\" style=\"font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,monospace;"
                + "font-size:16px;font-weight:700;letter-spacing:.5px;color:" + brand.getPrimaryColor() + ";\">"
                + escape(value) + "</span>";
    }

    /** Bulletproof, brand-gradient call-to-action button (centralizado). */
    public String button(String label, String url) {
        return "<table role=\"presentation\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:8px auto 6px;\">"
                + "<tr><td align=\"center\" bgcolor=\"" + brand.getPrimaryColor() + "\" "
                + "style=\"border-radius:11px;background:" + brand.getPrimaryColor()
                + ";background:linear-gradient(135deg," + brand.getPrimaryDark() + " 0%," + brand.getPrimaryColor()
                + " 60%," + brand.getAccentColor() + " 100%);\">"
                + "<a href=\"" + url + "\" target=\"_blank\" style=\"display:inline-block;padding:14px 30px;font-family:"
                + FONT_STACK + ";font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:11px;\">"
                + escape(label) + "</a></td></tr></table>";
    }

    /** Subtle left-accented note for security advice or secondary information. */
    public String callout(String html) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" "
                + "style=\"margin:18px 0;\"><tr><td class=\"ll-callout ll-text\" style=\"padding:14px 18px;text-align:center;background:"
                + brand.getInfoBackground()
                + ";border-left:3px solid " + brand.getPrimaryColor() + ";border-radius:0 10px 10px 0;font-family:"
                + FONT_STACK + ";font-size:14px;line-height:1.6;color:" + brand.getTextColor() + ";\">" + html
                + "</td></tr></table>";
    }

    // ========================== HELPERS ==========================

    /** Minimal HTML escaping for text injected into markup. */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
