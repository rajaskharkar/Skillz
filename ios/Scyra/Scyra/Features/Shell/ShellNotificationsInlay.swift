import SwiftUI

struct ShellNotificationsInlay: View {
    let notifications: [ShellNotificationItem]
    let onDismiss: () -> Void
    let onMarkAllViewed: () -> Void
    let onOpen: (ShellNotificationItem) -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black.opacity(0.001)
                .contentShape(Rectangle())
                .onTapGesture(perform: onDismiss)

            VStack(spacing: 0) {
                HStack(alignment: .center, spacing: 12) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Shell Notifications")
                            .font(.headline.weight(.semibold))
                        Text("New creatures and badges gather here until you visit.")
                            .font(.caption)
                            .foregroundStyle(ScyraColors.textSecondary)
                    }
                    Spacer(minLength: 4)
                    if !notifications.isEmpty {
                        Button("Mark Viewed", action: onMarkAllViewed)
                            .font(.subheadline)
                            .foregroundStyle(ScyraColors.primary)
                    }
                }
                .padding(.leading, 20)
                .padding(.top, 16)
                .padding(.trailing, 12)
                .padding(.bottom, 8)

                if notifications.isEmpty {
                    HStack(spacing: 16) {
                        ScyraCanonicalIcon(systemName: "bell")
                            .foregroundStyle(ScyraColors.secondaryGold)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("No new notifications")
                            Text("You’re all caught up.")
                                .font(.caption)
                                .foregroundStyle(ScyraColors.textSecondary)
                        }
                        Spacer()
                    }
                    .padding(20)
                } else {
                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(notifications) { notification in
                                Button { onOpen(notification) } label: {
                                    HStack(spacing: 14) {
                                        ScyraCanonicalIcon(systemName: notification.kind == .find ? "shippingbox" : "medal")
                                            .font(.title3)
                                            .foregroundStyle(ScyraColors.secondaryGold)
                                            .frame(width: 28)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(notification.title)
                                                .font(.body)
                                                .foregroundStyle(ScyraColors.textPrimary)
                                            Text(notification.detail)
                                                .font(.caption)
                                                .foregroundStyle(ScyraColors.textSecondary)
                                            Text(notification.occurredAt.formatted(date: .abbreviated, time: .shortened))
                                                .font(.caption2)
                                                .foregroundStyle(ScyraColors.textMuted)
                                        }
                                        Spacer()
                                        ScyraCanonicalIcon(systemName: "chevron.right")
                                            .font(.caption.weight(.semibold))
                                            .foregroundStyle(ScyraColors.textMuted)
                                    }
                                    .padding(12)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .background(ScyraColors.surface)
                                    .clipShape(RoundedRectangle(cornerRadius: ScyraRadius.card, style: .continuous))
                                    .shadow(color: Color.black.opacity(0.10), radius: 2, y: 1)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("\(notification.title). \(notification.detail)")
                            }
                        }
                        .padding(.horizontal, 12)
                        .padding(.bottom, 12)
                    }
                }
            }
            .frame(maxWidth: 380, maxHeight: 420)
            .background(ScyraColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .shadow(color: Color.black.opacity(0.25), radius: 12, y: 5)
            .padding(.top, 8)
            .padding(.horizontal, 12)
            .accessibilityElement(children: .contain)
            .accessibilityLabel("Shell notifications")
        }
    }
}
