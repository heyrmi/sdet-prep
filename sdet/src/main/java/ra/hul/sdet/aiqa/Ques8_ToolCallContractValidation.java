package ra.hul.sdet.aiqa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool-Call Contract Validation - the security boundary of an agent, tested.
 *
 * <p>An agent does not execute anything. It emits a structured REQUEST, and your runtime decides
 * whether to honour it. That decision point is where every safety control lives, and — happily for
 * a tester — it is entirely deterministic.
 *
 * <p>This validator enforces, in order:
 * <ol>
 *   <li><b>Schema</b> — the tool exists, required arguments are present, types are right, enums
 *       are respected.</li>
 *   <li><b>Authorisation</b> — the SESSION's identity and scopes permit this call. Never the
 *       model's reasoning. An injected instruction can be arbitrarily persuasive; it cannot
 *       change the session's scopes.</li>
 *   <li><b>Policy</b> — irreversible actions above a value threshold need human approval;
 *       arguments must fall inside sane bounds.</li>
 *   <li><b>Idempotency</b> — writes carry a key, because agents retry.</li>
 * </ol>
 *
 * <p>The headline test below is the one worth remembering: a tool call whose arguments are
 * perfectly schema-valid is still REJECTED, because the session lacks the scope. That is the
 * difference between a system that survives prompt injection and one that does not.
 *
 * <p>Self-contained: fixed tool definitions and simulated agent calls, no model.
 */
public class Ques8_ToolCallContractValidation {

    enum ArgType { STRING, NUMBER, BOOLEAN }

    /** One declared parameter of a tool. */
    record ParamSpec(String name, ArgType type, boolean required, Set<String> allowedValues) {
        static ParamSpec required(String name, ArgType type) {
            return new ParamSpec(name, type, true, Set.of());
        }

        static ParamSpec optional(String name, ArgType type) {
            return new ParamSpec(name, type, false, Set.of());
        }

        static ParamSpec enumOf(String name, Set<String> allowed) {
            return new ParamSpec(name, ArgType.STRING, true, allowed);
        }
    }

    /** A tool the agent may call. */
    record ToolSpec(
            String name,
            List<ParamSpec> params,
            /** Scope the caller's session must hold. */
            String requiredScope,
            /** Irreversible actions need extra controls. */
            boolean irreversible,
            /** Writes must carry an idempotency key, because agents retry. */
            boolean requiresIdempotencyKey) {
    }

    /** What the model emitted. */
    record ToolCall(String toolName, Map<String, Object> args, String idempotencyKey) {
    }

    /** The caller's authenticated session. This is ground truth; the model's claims are not. */
    record Session(String userId, Set<String> scopes, boolean humanApprovalGranted) {
    }

    enum Rejection { NONE, UNKNOWN_TOOL, SCHEMA, AUTHORIZATION, POLICY, IDEMPOTENCY }

    record Validation(boolean allowed, Rejection reason, String detail) {
        static Validation ok() {
            return new Validation(true, Rejection.NONE, "");
        }

        static Validation deny(Rejection reason, String detail) {
            return new Validation(false, reason, detail);
        }
    }

    /** Registry of permitted tools. */
    static Map<String, ToolSpec> registry() {
        Map<String, ToolSpec> tools = new LinkedHashMap<>();
        tools.put("search_orders", new ToolSpec("search_orders",
                List.of(ParamSpec.required("customer_id", ArgType.STRING),
                        ParamSpec.enumOf("status", Set.of("pending", "shipped", "delivered", "cancelled")),
                        ParamSpec.optional("limit", ArgType.NUMBER)),
                "orders:read", false, false));
        tools.put("issue_refund", new ToolSpec("issue_refund",
                List.of(ParamSpec.required("order_id", ArgType.STRING),
                        ParamSpec.required("amount_cents", ArgType.NUMBER)),
                "refunds:write", true, true));
        tools.put("close_ticket", new ToolSpec("close_ticket",
                List.of(ParamSpec.required("ticket_id", ArgType.STRING)),
                "tickets:write", false, true));
        return tools;
    }

    /** Refunds above this need a human, no matter how confident the agent is. */
    static final double HUMAN_APPROVAL_THRESHOLD_CENTS = 10_000;   // $100
    static final double MAX_REFUND_CENTS = 500_000;                // $5,000 hard ceiling

    static Validation validate(ToolCall call, Session session, Map<String, ToolSpec> tools) {
        // 1) Tool must be one we declared. An agent asking for a tool that does not exist is
        //    either confused or being steered.
        ToolSpec spec = tools.get(call.toolName());
        if (spec == null) {
            return Validation.deny(Rejection.UNKNOWN_TOOL, "no such tool: " + call.toolName());
        }

        // 2) Schema.
        for (ParamSpec p : spec.params()) {
            Object value = call.args().get(p.name());
            if (value == null) {
                if (p.required()) {
                    return Validation.deny(Rejection.SCHEMA, "missing required argument: " + p.name());
                }
                continue;
            }
            boolean typeOk = switch (p.type()) {
                case STRING -> value instanceof String;
                case NUMBER -> value instanceof Number;
                case BOOLEAN -> value instanceof Boolean;
            };
            if (!typeOk) {
                return Validation.deny(Rejection.SCHEMA,
                        p.name() + " must be " + p.type() + ", got " + value.getClass().getSimpleName());
            }
            if (!p.allowedValues().isEmpty() && !p.allowedValues().contains(String.valueOf(value))) {
                return Validation.deny(Rejection.SCHEMA,
                        p.name() + "=" + value + " is not one of " + p.allowedValues());
            }
        }
        for (String provided : call.args().keySet()) {
            boolean declared = spec.params().stream().anyMatch(p -> p.name().equals(provided));
            if (!declared) {
                // Undeclared arguments are how an injected instruction smuggles a parameter past
                // a validator that only checks the ones it knows about.
                return Validation.deny(Rejection.SCHEMA, "undeclared argument: " + provided);
            }
        }

        // 3) Authorisation — against the SESSION, never the model's justification.
        if (!session.scopes().contains(spec.requiredScope())) {
            return Validation.deny(Rejection.AUTHORIZATION,
                    "session lacks scope " + spec.requiredScope());
        }

        // 4) Policy.
        if (spec.name().equals("issue_refund")) {
            double amount = ((Number) call.args().get("amount_cents")).doubleValue();
            if (amount <= 0) {
                return Validation.deny(Rejection.POLICY, "refund amount must be positive");
            }
            if (amount > MAX_REFUND_CENTS) {
                return Validation.deny(Rejection.POLICY,
                        String.format("refund of %.0f cents exceeds the hard ceiling of %.0f",
                                amount, MAX_REFUND_CENTS));
            }
            if (amount > HUMAN_APPROVAL_THRESHOLD_CENTS && !session.humanApprovalGranted()) {
                return Validation.deny(Rejection.POLICY,
                        String.format("refund of %.0f cents needs human approval", amount));
            }
        }

        // 5) Idempotency — agents retry, and a duplicate refund is a real incident.
        if (spec.requiresIdempotencyKey()
                && (call.idempotencyKey() == null || call.idempotencyKey().isBlank())) {
            return Validation.deny(Rejection.IDEMPOTENCY,
                    spec.name() + " is a write and must carry an idempotency key");
        }

        return Validation.ok();
    }

    static void main() {
        int passed = 0, failed = 0;
        Map<String, ToolSpec> tools = registry();

        Session support = new Session("agent-7",
                Set.of("orders:read", "tickets:write", "refunds:write"), false);
        Session readOnly = new Session("agent-9", Set.of("orders:read"), false);
        Session approved = new Session("agent-7",
                Set.of("orders:read", "refunds:write"), true);

        List<String> log = new ArrayList<>();

        System.out.println("=== Tool-call contract validation ===\n");

        // 1. Valid read.
        Validation v = validate(new ToolCall("search_orders",
                Map.of("customer_id", "cust-1", "status", "shipped"), null), support, tools);
        boolean c1 = v.allowed();
        log.add(String.format("valid read call allowed                        : %s", c1));
        if (c1) passed++; else failed++;

        // 2. Unknown tool.
        v = validate(new ToolCall("delete_all_orders", Map.of(), "k1"), support, tools);
        boolean c2 = !v.allowed() && v.reason() == Rejection.UNKNOWN_TOOL;
        log.add(String.format("undeclared tool rejected                       : %s", c2));
        if (c2) passed++; else failed++;

        // 3. Missing required argument.
        v = validate(new ToolCall("search_orders", Map.of("status", "shipped"), null), support, tools);
        boolean c3 = !v.allowed() && v.reason() == Rejection.SCHEMA;
        log.add(String.format("missing required arg rejected (%s)         : %s", "schema", c3));
        if (c3) passed++; else failed++;

        // 4. Enum violation — a very common real failure when the schema lacks enums.
        v = validate(new ToolCall("search_orders",
                Map.of("customer_id", "c1", "status", "in-transit"), null), support, tools);
        boolean c4 = !v.allowed() && v.reason() == Rejection.SCHEMA;
        log.add(String.format("invalid enum value rejected                    : %s", c4));
        if (c4) passed++; else failed++;

        // 5. Wrong type.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", "five hundred"), "k1"), support, tools);
        boolean c5 = !v.allowed() && v.reason() == Rejection.SCHEMA;
        log.add(String.format("wrong argument type rejected                   : %s", c5));
        if (c5) passed++; else failed++;

        // 6. Undeclared extra argument.
        v = validate(new ToolCall("search_orders",
                Map.of("customer_id", "c1", "status", "shipped", "bypass_auth", true), null),
                support, tools);
        boolean c6 = !v.allowed() && v.reason() == Rejection.SCHEMA;
        log.add(String.format("undeclared extra argument rejected             : %s", c6));
        if (c6) passed++; else failed++;

        // 7. THE headline case: schema-perfect call, wrong session scope.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", 500), "k1"), readOnly, tools);
        boolean c7 = !v.allowed() && v.reason() == Rejection.AUTHORIZATION;
        log.add(String.format("schema-VALID refund denied on session scope    : %s", c7));
        if (c7) passed++; else failed++;

        // 8. Human approval threshold.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", 50_000), "k1"), support, tools);
        boolean c8 = !v.allowed() && v.reason() == Rejection.POLICY;
        log.add(String.format("$500 refund without approval denied            : %s", c8));
        if (c8) passed++; else failed++;

        // 9. Same call, approval granted.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", 50_000), "k1"), approved, tools);
        boolean c9 = v.allowed();
        log.add(String.format("same refund allowed once a human approved      : %s", c9));
        if (c9) passed++; else failed++;

        // 10. Hard ceiling — the classic injection payload.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", 1_000_000), "k1"), approved, tools);
        boolean c10 = !v.allowed() && v.reason() == Rejection.POLICY;
        log.add(String.format("$10,000 refund denied by the hard ceiling      : %s", c10));
        if (c10) passed++; else failed++;

        // 11. Missing idempotency key on a write.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", 500), null), support, tools);
        boolean c11 = !v.allowed() && v.reason() == Rejection.IDEMPOTENCY;
        log.add(String.format("write without idempotency key rejected         : %s", c11));
        if (c11) passed++; else failed++;

        // 12. Negative amount.
        v = validate(new ToolCall("issue_refund",
                Map.of("order_id", "o1", "amount_cents", -100), "k1"), support, tools);
        boolean c12 = !v.allowed() && v.reason() == Rejection.POLICY;
        log.add(String.format("negative refund amount rejected                : %s", c12));
        if (c12) passed++; else failed++;

        log.forEach(System.out::println);

        System.out.println("\n--- why this is the security boundary ---");
        System.out.println("Check 7 is the one that matters. The agent produced a perfectly");
        System.out.println("well-formed refund call — correct tool, correct types, sane amount.");
        System.out.println("It was still denied, because the SESSION lacked refunds:write.");
        System.out.println();
        System.out.println("A prompt injection can make the model believe anything and phrase the");
        System.out.println("request impeccably. It cannot grant the session a scope it does not");
        System.out.println("hold. That is why 'write a better system prompt' is the wrong answer");
        System.out.println("to injection, and runtime authorisation is the right one.");

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        System.out.println(failed == 0
                ? "PASSED: contract validation enforces schema, authorisation, policy, and idempotency."
                : "FAIL: tool-call contract validation mismatch.");
    }
}
