package com.labhub.server.handler;

import com.labhub.common.dto.AuditLogDto;
import com.labhub.common.dto.DashboardStatsDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import com.labhub.common.protocol.Request;
import com.labhub.common.protocol.Response;
import com.labhub.server.repository.DashboardRepository;

/**
 * Handles DASHBOARD_STATS, DASHBOARD_ACTIVITY, and AUDIT_LIST requests.
 */
public class DashboardHandler extends BaseHandler {

    private final DashboardRepository dashboardRepository;

    /**
     * @param dashboardRepository stats and audit data access
     */
    public DashboardHandler(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public Response handle(Request request, UserDto currentUser) {
        return switch (request.getType()) {
            case DASHBOARD_STATS    -> handleStats(request);
            case DASHBOARD_ACTIVITY,
                 AUDIT_LIST         -> handleAuditList(request, currentUser);
            default -> Response.error(request.getRequestId(), "INVALID_REQUEST",
                    "DashboardHandler does not handle " + request.getType());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private Response handleStats(Request request) {
        DashboardStatsDto stats = dashboardRepository.getStats();
        return Response.ok(request.getRequestId(), "Dashboard stats loaded.", toJsonNode(stats));
    }

    private Response handleAuditList(Request request, UserDto currentUser) {
        // Only admins and lab managers can read audit logs
        Response authError = requireRole(currentUser, request.getRequestId(),
                UserRole.ADMIN, UserRole.LAB_MANAGER);
        if (authError != null) {
            return authError;
        }
        int page     = intOrDefault(request.getPayload(), "page",     1);
        int pageSize = intOrDefault(request.getPayload(), "pageSize", 50);
        PagedResult<AuditLogDto> result = dashboardRepository.findAuditLogs(null, null, page, pageSize);
        return Response.ok(request.getRequestId(), "Audit log loaded.", toJsonNode(result));
    }
}
