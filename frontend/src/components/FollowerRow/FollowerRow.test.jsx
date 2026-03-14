import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import FollowerRow from "./FollowerRow";

const mockUserData = {
  id: 2,
  username: "milanavram",
  fname: "Milan",
  lname: "Avramovic",
  profilePictureUrl: null,
};

const mockOnFollow = vi.fn();
const mockOnRemove = vi.fn();
const mockOnNavigate = vi.fn();

const renderFollowerRow = async (props = {}) => {
  await act(async () => {
    render(
      <MemoryRouter>
        <FollowerRow
          userId={2}
          isFollowing={false}
          isOwnProfile={false}
          isFollowersList={false}
          isMe={false}
          onFollow={mockOnFollow}
          onRemove={mockOnRemove}
          onNavigate={mockOnNavigate}
          {...props}
        />
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(mockUserData),
    }),
  );
  localStorage.setItem("token", "test-token");
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe("FollowerRow", () => {
  it("prikazuje username korisnika", async () => {
    await renderFollowerRow();
    expect(screen.getByText("milanavram")).toBeInTheDocument();
  });

  it("prikazuje dugme Прати kad nije zapracen", async () => {
    await renderFollowerRow({ isFollowing: false });
    expect(screen.getByText("Прати")).toBeInTheDocument();
  });

  it("prikazuje dugme Отпрати kad je zapracen", async () => {
    await renderFollowerRow({ isFollowing: true });
    expect(screen.getByText("Отпрати")).toBeInTheDocument();
  });

  it("poziva onFollow klikom na Прати", async () => {
    await renderFollowerRow({ isFollowing: false });
    fireEvent.click(screen.getByText("Прати"));
    expect(mockOnFollow).toHaveBeenCalled();
  });

  it("prikazuje dugme Уклони za sopstveni profil u listi pratilaca", async () => {
    await renderFollowerRow({ isOwnProfile: true, isFollowersList: true });
    expect(screen.getByText("Уклони")).toBeInTheDocument();
  });

  it("poziva onRemove klikom na Уклони", async () => {
    await renderFollowerRow({ isOwnProfile: true, isFollowersList: true });
    fireEvent.click(screen.getByText("Уклони"));
    expect(mockOnRemove).toHaveBeenCalled();
  });

  it("ne prikazuje dugmice ako je isMe true", async () => {
    await renderFollowerRow({ isMe: true });
    expect(screen.queryByText("Прати")).not.toBeInTheDocument();
    expect(screen.queryByText("Уклони")).not.toBeInTheDocument();
  });

  it("poziva onNavigate klikom na username", async () => {
    await renderFollowerRow();
    fireEvent.click(screen.getByText("milanavram"));
    expect(mockOnNavigate).toHaveBeenCalled();
  });
});
