import { render, screen, cleanup, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import Notification from "./Notification";

vi.mock("../NotificationItem/NotificationItem", () => ({
  default: ({ request }) => (
    <div data-testid="notification-item">{request.type}</div>
  ),
}));

const mockSetSearchNotification = vi.fn();

const mockPending = [{ id: 1, senderId: 2, type: "request" }];

const mockNotifications = [{ id: 2, senderId: 3, type: "follow", read: false }];

const mockUserData = {
  id: 2,
  username: "otheruser",
  profilePictureUrl: null,
};

const renderNotification = async () => {
  await act(async () => {
    render(<Notification setSearchNotification={mockSetSearchNotification} />);
  });
};

beforeEach(() => {
  global.fetch = vi.fn((url) => {
    if (url.includes("/requests/pending")) {
      return Promise.resolve({
        json: () => Promise.resolve(mockPending),
      });
    }
    if (url.includes("/notifications")) {
      return Promise.resolve({
        json: () => Promise.resolve(mockNotifications),
      });
    }
    if (url.includes("/api/v1/user/id/")) {
      return Promise.resolve({
        json: () => Promise.resolve(mockUserData),
      });
    }
    return Promise.resolve({ json: () => Promise.resolve({}) });
  });
  localStorage.setItem("token", "test-token");
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe("Notification", () => {
  it("prikazuje naslov Обавештења", async () => {
    await renderNotification();
    expect(screen.getByText("Обавештења")).toBeInTheDocument();
  });

  it("prikazuje poruku kad nema obavestenja", async () => {
    global.fetch = vi.fn((url) => {
      if (url.includes("/requests/pending")) {
        return Promise.resolve({ json: () => Promise.resolve([]) });
      }
      if (url.includes("/notifications")) {
        return Promise.resolve({ json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ json: () => Promise.resolve({}) });
    });
    await renderNotification();
    expect(screen.getByText("Нема нових обавештења")).toBeInTheDocument();
  });

  it("prikazuje notification iteme", async () => {
    await renderNotification();
    expect(screen.getAllByTestId("notification-item").length).toBeGreaterThan(
      0,
    );
  });

  it("prikazuje zahteve za pracenje", async () => {
    await renderNotification();
    expect(screen.getByText("request")).toBeInTheDocument();
  });
});
