package com.digitalmarket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class DigitalMarketingApp {
    private static final List<Campaign> campaigns = new ArrayList<>(List.of(
            new Campaign("Summer launch", "Social media", new BigDecimal("2400.00")),
            new Campaign("Search essentials", "Search ads", new BigDecimal("1800.00")),
            new Campaign("Weekly newsletter", "Email", new BigDecimal("650.00"))
    ));

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", DigitalMarketingApp::handleRequest);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Digital Marketing Studio is running at http://localhost:" + port);
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("/".equals(exchange.getRequestURI().getPath())
                    && "GET".equals(exchange.getRequestMethod())) {
                sendHtml(exchange, 200, renderPage(""));
            } else if ("/".equals(exchange.getRequestURI().getPath())
                    && "POST".equals(exchange.getRequestMethod())) {
                handleNewCampaign(exchange);
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                sendText(exchange, 405, "Method not allowed");
            }
        }
    }

    private static void handleNewCampaign(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readNBytes(8193), StandardCharsets.UTF_8);
        if (body.length() > 8192) {
            sendHtml(exchange, 413, renderPage("The form submission is too large."));
            return;
        }

        Map<String, String> form = parseForm(body);
        String name = form.getOrDefault("name", "").trim();
        String channel = form.getOrDefault("channel", "");
        String budgetInput = form.getOrDefault("budget", "").trim();
        BigDecimal budget;

        try {
            budget = new BigDecimal(budgetInput);
            if (budget.signum() <= 0 || budget.scale() > 2
                    || budget.compareTo(new BigDecimal("1000000")) > 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            sendHtml(exchange, 400, renderPage("Enter a monthly budget from $0.01 to $1,000,000 with up to 2 decimal places."));
            return;
        }

        if (name.isBlank() || name.length() > 80) {
            sendHtml(exchange, 400, renderPage("Enter a campaign name of 1 to 80 characters."));
            return;
        }
        if (!List.of("Social media", "Search ads", "Email", "Content marketing").contains(channel)) {
            sendHtml(exchange, 400, renderPage("Choose a valid marketing channel."));
            return;
        }

        synchronized (campaigns) {
            campaigns.add(new Campaign(name, channel, budget));
        }
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
            values.put(key, value);
        }
        return values;
    }

    private static String renderPage(String errorMessage) {
        List<Campaign> currentCampaigns;
        synchronized (campaigns) {
            currentCampaigns = List.copyOf(campaigns);
        }
        BigDecimal totalBudget = currentCampaigns.stream()
                .map(Campaign::budget)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder rows = new StringBuilder();
        for (Campaign campaign : currentCampaigns) {
            rows.append("""
                    <tr class="campaign-row" data-name="%s" data-channel="%s" data-budget="%s">
                      <td><span class="campaign-name">%s</span><span class="campaign-sub">Always-on campaign</span></td>
                      <td><span class="channel-tag">%s</span></td>
                      <td><span class="status"><span></span> Active</span></td>
                      <td class="budget">$%s<span class="per-month">/ month</span></td>
                    </tr>
                    """.formatted(escapeHtml(campaign.name()), escapeHtml(campaign.channel()),
                    campaign.budget().toPlainString(), escapeHtml(campaign.name()),
                    escapeHtml(campaign.channel()), campaign.budget().toPlainString()));
        }

        String error = errorMessage.isEmpty() ? "" : """
                <div class="error" role="alert">%s</div>
                """.formatted(escapeHtml(errorMessage));

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="theme-color" content="#f4f5fb">
                  <title>Marketly — Campaign overview</title>
                  <style>
                    :root { color-scheme: light; --ink: #20213a; --muted: #85869c; --line: #ececf3; --purple: #7357ee; --surface: #fff; --page: #f5f5fa; --soft: #f2efff; --green: #168c70; }
                    * { box-sizing: border-box; }
                    *:focus-visible { outline: 3px solid #bdb1ff; outline-offset: 3px; }
                    body { margin: 0; background: var(--page); color: var(--ink); font: 14px/1.55 Inter, ui-sans-serif, system-ui, -apple-system, "Segoe UI", sans-serif; transition: background .2s, color .2s; }
                    body.dark-mode { color-scheme: dark; --ink: #f0effb; --muted: #a6a4ba; --line: #333149; --surface: #242237; --page: #1b1a2a; --soft: #37304f; }
                    .layout { display: flex; min-height: 100vh; }
                    aside { position: sticky; top: 0; width: 246px; height: 100vh; padding: 27px 18px; background: var(--surface); border-right: 1px solid var(--line); flex-shrink: 0; }
                    .brand { display: flex; gap: 11px; align-items: center; padding: 0 10px 38px; font-size: 17px; font-weight: 780; letter-spacing: -.5px; }
                    .brand-mark { width: 35px; height: 35px; display: grid; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(145deg, #a68aff, #6847e9); box-shadow: 0 5px 12px #7655e944; font-size: 19px; }
                    .nav-label { padding: 0 11px; margin: 5px 0 12px; color: var(--muted); font-size: 10px; font-weight: 750; letter-spacing: 1.2px; text-transform: uppercase; }
                    .nav-item { display: flex; gap: 12px; align-items: center; margin: 5px 0; padding: 11px; color: var(--muted); border-radius: 9px; text-decoration: none; transition: .18s ease; }
                    .nav-item:hover, .nav-item.active { color: var(--purple); background: var(--soft); font-weight: 650; transform: translateX(2px); }
                    .nav-icon { width: 18px; text-align: center; font-size: 16px; }
                    .sidebar-note { position: absolute; right: 18px; bottom: 20px; left: 18px; padding: 14px; color: var(--muted); background: var(--page); border-radius: 11px; font-size: 11px; }
                    .sidebar-note strong { display: block; margin-bottom: 4px; color: var(--ink); font-size: 12px; }
                    main { width: min(1220px, 100%); padding: 34px 48px 58px; margin: 0 auto; }
                    .topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 38px; }
                    .breadcrumb { color: var(--muted); font-size: 12px; }
                    .top-actions { display: flex; gap: 12px; align-items: center; }
                    .theme-toggle { width: 38px; height: 38px; padding: 0; color: var(--ink); background: var(--surface); border: 1px solid var(--line); border-radius: 11px; font-size: 16px; }
                    .avatar { width: 39px; height: 39px; display: grid; place-items: center; border: 2px solid var(--surface); border-radius: 50%; color: #684fdd; background: #e9e5ff; box-shadow: 0 0 0 1px var(--line); font-weight: 750; }
                    h1 { margin: 0; font-size: clamp(25px, 3vw, 32px); letter-spacing: -1.2px; line-height: 1.2; }
                    .intro { margin: 8px 0 0; color: var(--muted); }
                    .overview { display: flex; justify-content: space-between; align-items: end; gap: 18px; margin-bottom: 26px; }
                    .date-pill { padding: 9px 13px; color: var(--muted); background: var(--surface); border: 1px solid var(--line); border-radius: 9px; font-size: 12px; }
                    .metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 17px; margin-bottom: 23px; }
                    .metric, .panel { background: var(--surface); border: 1px solid var(--line); border-radius: 15px; box-shadow: 0 5px 18px #25204408; }
                    .metric { position: relative; overflow: hidden; padding: 19px 21px; transition: transform .18s ease, box-shadow .18s ease; }
                    .metric:hover { transform: translateY(-3px); box-shadow: 0 12px 24px #25204412; }
                    .metric-label { color: var(--muted); font-size: 12px; }
                    .metric-value { margin-top: 7px; font-size: 27px; font-weight: 760; letter-spacing: -.8px; }
                    .metric-note { margin-top: 4px; color: var(--muted); font-size: 11px; }
                    .metric-note .up { color: var(--green); font-weight: 700; }
                    .metric-icon { position: absolute; top: 17px; right: 18px; width: 34px; height: 34px; display: grid; place-items: center; color: var(--purple); background: var(--soft); border-radius: 10px; }
                    .panel { padding: 21px; }
                    .panel-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
                    h2 { margin: 0; font-size: 16px; letter-spacing: -.3px; }
                    .count { margin-left: 7px; padding: 3px 8px; color: var(--muted); background: var(--page); border-radius: 20px; font-size: 11px; font-weight: 550; }
                    .toolbar { display: flex; gap: 9px; align-items: center; }
                    .search-box { position: relative; }
                    .search-box span { position: absolute; top: 8px; left: 11px; color: var(--muted); }
                    .search-box input { padding-left: 31px; }
                    .table-wrap { overflow-x: auto; }
                    table { width: 100%; border-collapse: collapse; text-align: left; white-space: nowrap; }
                    th { padding: 0 12px 11px; color: var(--muted); font-size: 10px; font-weight: 700; letter-spacing: .8px; text-transform: uppercase; }
                    td { padding: 14px 12px; border-top: 1px solid var(--line); }
                    tbody tr { transition: background .15s; }
                    tbody tr:hover { background: var(--page); }
                    .campaign-name, .campaign-sub { display: block; }
                    .campaign-name { font-weight: 670; }
                    .campaign-sub, .per-month { color: var(--muted); font-size: 11px; }
                    .channel-tag { padding: 5px 9px; color: var(--purple); background: var(--soft); border-radius: 7px; font-size: 11px; font-weight: 600; }
                    .status { display: inline-flex; gap: 7px; align-items: center; color: var(--green); font-size: 12px; font-weight: 550; }
                    .status span { width: 7px; height: 7px; background: #42b990; border-radius: 50%; box-shadow: 0 0 0 3px #42b99022; }
                    .budget { font-weight: 680; }
                    .per-month { margin-left: 4px; font-weight: 400; }
                    .form-panel { margin-top: 19px; }
                    .form-grid { display: grid; grid-template-columns: 1.3fr 1fr 1fr auto; gap: 12px; align-items: end; }
                    label { display: block; margin-bottom: 6px; color: var(--muted); font-size: 11px; font-weight: 650; }
                    input, select { width: 100%; height: 41px; padding: 0 11px; color: var(--ink); background: var(--surface); border: 1px solid var(--line); border-radius: 9px; font: inherit; font-size: 12px; }
                    input::placeholder { color: #a8a7b8; }
                    input:focus, select:focus { border-color: var(--purple); outline: 2px solid #eeeaff; }
                    button { min-height: 41px; padding: 0 15px; color: #fff; background: linear-gradient(135deg, #846bfa, #6749e8); border: 0; border-radius: 9px; font: inherit; font-size: 12px; font-weight: 680; cursor: pointer; white-space: nowrap; box-shadow: 0 5px 12px #7357ee30; transition: transform .15s, box-shadow .15s; }
                    button:hover { transform: translateY(-1px); box-shadow: 0 8px 16px #7357ee3d; }
                    button:active { transform: translateY(0); }
                    .error { margin: 0 0 16px; padding: 10px 12px; color: #a53d48; background: #fff1f1; border: 1px solid #f4dada; border-radius: 9px; font-size: 12px; }
                    .empty-state { padding: 24px; color: var(--muted); text-align: center; }
                    .footer-note { margin-top: 20px; color: var(--muted); font-size: 11px; text-align: center; }
                    @media (max-width: 900px) { aside { width: 205px; } main { padding: 28px 25px 42px; } .form-grid { grid-template-columns: 1fr 1fr; } }
                    @media (max-width: 650px) { aside { display: none; } main { padding: 22px 15px 36px; } .topbar { margin-bottom: 27px; } .metrics { grid-template-columns: 1fr; gap: 10px; } .metric { padding: 15px 18px; } .metric-value { font-size: 23px; } .overview { align-items: start; flex-direction: column; margin-bottom: 20px; } .panel { padding: 16px 13px; } .form-grid { grid-template-columns: 1fr; } .panel-heading { align-items: flex-start; flex-direction: column; } .toolbar { width: 100%; } .toolbar > * { flex: 1; min-width: 0; } .theme-toggle { flex: 0 0 38px; } }
                    @media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; animation-duration: .01ms !important; } }
                  </style>
                  <script>
                    function setTheme(isDark) {
                      document.body.classList.toggle('dark-mode', isDark);
                      const button = document.getElementById('theme-toggle');
                      button.textContent = isDark ? '☀' : '☾';
                      button.setAttribute('aria-pressed', String(isDark));
                      localStorage.setItem('marketly-dark-mode', String(isDark));
                    }
                    document.addEventListener('DOMContentLoaded', () => {
                      const search = document.getElementById('campaign-search');
                      const channelFilter = document.getElementById('channel-filter');
                      const tableBody = document.getElementById('campaign-rows');
                      const emptyState = document.getElementById('empty-state');
                      const campaignCount = document.getElementById('campaign-count');
                      const themeToggle = document.getElementById('theme-toggle');
                      const rows = Array.from(tableBody.querySelectorAll('.campaign-row'));
                      const savedTheme = localStorage.getItem('marketly-dark-mode') === 'true';
                      setTheme(savedTheme);
                      themeToggle.addEventListener('click', () => {
                        setTheme(!document.body.classList.contains('dark-mode'));
                      });
                      function filterCampaigns() {
                        const query = search.value.trim().toLowerCase();
                        const selectedChannel = channelFilter.value;
                        let visibleCount = 0;
                        rows.forEach(row => {
                          const matches = row.dataset.name.toLowerCase().includes(query)
                            && (!selectedChannel || row.dataset.channel === selectedChannel);
                          row.hidden = !matches;
                          if (matches) visibleCount++;
                        });
                        campaignCount.textContent = `${visibleCount} shown`;
                        emptyState.hidden = visibleCount !== 0;
                      }
                      search.addEventListener('input', filterCampaigns);
                      channelFilter.addEventListener('change', filterCampaigns);
                      document.getElementById('sort-budget').addEventListener('click', event => {
                        const ascending = event.currentTarget.dataset.order !== 'ascending';
                        event.currentTarget.dataset.order = ascending ? 'ascending' : 'descending';
                        event.currentTarget.textContent = ascending ? 'Budget ↑' : 'Budget ↓';
                        rows.sort((a, b) => (Number(a.dataset.budget) - Number(b.dataset.budget)) * (ascending ? 1 : -1));
                        rows.forEach(row => tableBody.appendChild(row));
                        filterCampaigns();
                      });
                    });
                  </script>
                </head>
                <body>
                  <div class="layout">
                    <aside>
                      <div class="brand"><span class="brand-mark">✳</span> marketly</div>
                      <div class="nav-label">Workspace</div>
                      <a class="nav-item active" href="#overview"><span class="nav-icon">▦</span> Overview</a>
                      <a class="nav-item" href="#campaigns"><span class="nav-icon">◈</span> Campaigns</a>
                      <a class="nav-item" href="#new-campaign"><span class="nav-icon">＋</span> Create campaign</a>
                      <div class="nav-label" style="margin-top: 32px">Your space</div>
                      <a class="nav-item" href="#campaigns"><span class="nav-icon">▤</span> Reports</a>
                      <a class="nav-item" href="#new-campaign"><span class="nav-icon">⚙</span> Settings</a>
                      <div class="sidebar-note"><strong>Your growth, at a glance</strong>Keep your campaigns organized and your next big idea moving.</div>
                    </aside>
                    <main id="overview">
                      <div class="topbar"><div class="breadcrumb">Workspace &nbsp;/&nbsp; Overview</div><div class="top-actions"><button class="theme-toggle" id="theme-toggle" type="button" aria-label="Toggle dark mode" aria-pressed="false">☾</button><div class="avatar" aria-label="Account: Alex Morgan">AM</div></div></div>
                      <div class="overview">
                        <div><h1>Good morning, Alex 👋</h1><p class="intro">Here’s what’s happening with your marketing today.</p></div>
                        <div class="date-pill">◷ &nbsp; Last 30 days &nbsp;⌄</div>
                      </div>
                      <section class="metrics" aria-label="Marketing performance">
                        <article class="metric"><span class="metric-icon">＄</span><div class="metric-label">Total campaign budget</div><div class="metric-value">$__TOTAL_BUDGET__</div><div class="metric-note">Monthly across __CAMPAIGN_COUNT__ active campaigns</div></article>
                        <article class="metric"><span class="metric-icon">◉</span><div class="metric-label">Website visitors</div><div class="metric-value">12,840</div><div class="metric-note"><span class="up">↑ 18%</span> compared with last month</div></article>
                        <article class="metric"><span class="metric-icon">✳</span><div class="metric-label">New leads</div><div class="metric-value">386</div><div class="metric-note"><span class="up">↑ 12%</span> compared with last month</div></article>
                      </section>
                      <section class="panel" id="campaigns">
                        <div class="panel-heading">
                          <h2>Your campaigns <span class="count" id="campaign-count">__CAMPAIGN_COUNT__ shown</span></h2>
                          <div class="toolbar">
                            <label class="search-box"><span aria-hidden="true">⌕</span><input id="campaign-search" type="search" placeholder="Search campaigns..." aria-label="Search campaigns"></label>
                            <select id="channel-filter" aria-label="Filter by channel"><option value="">All channels</option><option>Social media</option><option>Search ads</option><option>Email</option><option>Content marketing</option></select>
                            <button id="sort-budget" type="button" data-order="descending">Budget ↕</button>
                          </div>
                        </div>
                        <div class="table-wrap"><table>
                          <thead><tr><th>Campaign</th><th>Channel</th><th>Status</th><th>Monthly budget</th></tr></thead>
                          <tbody id="campaign-rows">__CAMPAIGN_ROWS__</tbody>
                        </table></div>
                        <div class="empty-state" id="empty-state" hidden>No campaigns match your search. Try another name or channel.</div>
                      </section>
                      <section class="panel form-panel" id="new-campaign">
                        <div class="panel-heading"><h2>Create a campaign</h2></div>
                        __ERROR_MESSAGE__
                        <form method="post" action="/">
                          <div class="form-grid">
                            <div><label for="name">Campaign name</label><input id="name" name="name" maxlength="80" placeholder="e.g. Product launch" required></div>
                            <div><label for="channel">Marketing channel</label><select id="channel" name="channel" required><option value="">Choose channel</option><option>Social media</option><option>Search ads</option><option>Email</option><option>Content marketing</option></select></div>
                            <div><label for="budget">Monthly budget ($)</label><input id="budget" name="budget" type="number" min="0.01" max="1000000" step="0.01" placeholder="500" required></div>
                            <button type="submit">＋ &nbsp; Add campaign</button>
                          </div>
                        </form>
                      </section>
                      <div class="footer-note">Marketly · Simple marketing insights for growing teams</div>
                    </main>
                  </div>
                </body>
                </html>
                """
                .replace("__TOTAL_BUDGET__", totalBudget.toPlainString())
                .replace("__CAMPAIGN_COUNT__", Integer.toString(currentCampaigns.size()))
                .replace("__CAMPAIGN_ROWS__", rows.toString())
                .replace("__ERROR_MESSAGE__", error);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
    }

    private static void sendText(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
    }

    private record Campaign(String name, String channel, BigDecimal budget) {}
}
