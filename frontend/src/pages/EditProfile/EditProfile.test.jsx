import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import EditProfile from "./EditProfile";
import { AppContext } from "../../context/AppContext";

vi.mock("../../api/userApi", () => ({
  getUserAvatarUrl: vi.fn(() => "mocked-avatar-url"),
  fetchProfileInfo: vi.fn(() => Promise.resolve({})),
  updateProfile: vi.fn(() => Promise.resolve({ ok: true })),
  updateProfilePicture: vi.fn(() => Promise.resolve()),
}));

vi.mock("../../api/authApi", () => ({
  deleteAccount: vi.fn(() => Promise.resolve({ ok: true })),
}));

vi.mock("../../assets/assets", () => ({
  assets: { noProfilePic: "mocked-no-profile-pic" },
}));

const mockUser = {
  id: 1,
  username: "mihajlotim",
  fname: "Mihajlo",
  lname: "Timotijevic",
  bio: "Ja sam Mihajlo i vozim biciklu.",
  privateProfile: false,
  profilePictureUrl: null,
};

const mockUpdateUser = vi.fn();
const mockLogout = vi.fn();

const renderEditProfile = async (user = mockUser) => {
  await act(async () => {
    render(
      <MemoryRouter>
        <AppContext.Provider
          value={{ user, updateUser: mockUpdateUser, logout: mockLogout }}
        >
          <EditProfile />
        </AppContext.Provider>
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  localStorage.setItem("token", "test-token");
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  vi.useRealTimers();
  localStorage.clear();
});

describe("EditProfile", () => {
  it("prikazuje formu za izmenu profila", async () => {
    await renderEditProfile();
    expect(screen.getByText("Измените профил")).toBeInTheDocument();
    expect(screen.getByText("Биографија")).toBeInTheDocument();
    expect(screen.getByText("Име")).toBeInTheDocument();
    expect(screen.getByText("Презиме")).toBeInTheDocument();
    expect(screen.getByText("Корисничко име")).toBeInTheDocument();
    expect(screen.getByText("Пошаљи")).toBeInTheDocument();
    expect(screen.getByText("Обришите налог")).toBeInTheDocument();
  });

  it("prikazuje postojece vrednosti korisnika", async () => {
    await renderEditProfile();
    expect(screen.getByPlaceholderText("Биографија")).toHaveValue(
      "Ja sam Mihajlo i vozim biciklu.",
    );
    expect(screen.getByPlaceholderText("Име")).toHaveValue("Mihajlo");
    expect(screen.getByPlaceholderText("Презиме")).toHaveValue("Timotijevic");
    expect(screen.getByPlaceholderText("Корисничко име")).toHaveValue(
      "mihajlotim",
    );
  });

  it("menja vrednost bio polja", async () => {
    await renderEditProfile();
    const bioInput = screen.getByPlaceholderText("Биографија");
    fireEvent.change(bioInput, { target: { value: "Nova biografija" } });
    expect(bioInput).toHaveValue("Nova biografija");
  });

  it("menja vrednost fname polja", async () => {
    await renderEditProfile();
    const fnameInput = screen.getByPlaceholderText("Име");
    fireEvent.change(fnameInput, { target: { value: "Novo ime" } });
    expect(fnameInput).toHaveValue("Novo ime");
  });

  it("menja vrednost lname polja", async () => {
    await renderEditProfile();
    const lnameInput = screen.getByPlaceholderText("Презиме");
    fireEvent.change(lnameInput, { target: { value: "Novo prezime" } });
    expect(lnameInput).toHaveValue("Novo prezime");
  });

  it("menja vrednost username polja", async () => {
    await renderEditProfile();
    const usernameInput = screen.getByPlaceholderText("Корисничко име");
    fireEvent.change(usernameInput, { target: { value: "noviusername" } });
    expect(usernameInput).toHaveValue("noviusername");
  });

  it("menja privatnost naloga", async () => {
    await renderEditProfile();
    const checkbox = screen.getByRole("checkbox");
    expect(checkbox).not.toBeChecked();
    fireEvent.click(checkbox);
    expect(checkbox).toBeChecked();
  });

  it("poziva updateProfile pri submit", async () => {
    const { updateProfile } = await import("../../api/userApi");
    await renderEditProfile();
    await act(async () => {
      fireEvent.click(screen.getByText("Пошаљи"));
    });
    expect(updateProfile).toHaveBeenCalledWith(1, {
      fname: "Mihajlo",
      lname: "Timotijevic",
      username: "mihajlotim",
      bio: "Ja sam Mihajlo i vozim biciklu.",
      privateProfile: false,
    });
  });

  it("poziva logout i redirect kada se promeni username", async () => {
    vi.useFakeTimers();

    await renderEditProfile();
    const usernameInput = screen.getByPlaceholderText("Корисничко име");
    fireEvent.change(usernameInput, { target: { value: "noviusername" } });

    await act(async () => {
      fireEvent.click(screen.getByText("Пошаљи"));
    });

    await act(async () => {
      vi.advanceTimersByTime(2000);
    });

    expect(mockLogout).toHaveBeenCalled();
  });

  it("prikazuje CustomConfirm pri brisanju naloga", async () => {
    await renderEditProfile();
    await act(async () => {
      fireEvent.click(screen.getByText("Обришите налог"));
    });
    expect(
      screen.getByText("Да ли сте сигурни да желите да обришете налог?"),
    ).toBeInTheDocument();
  });

  it("odustaje od brisanja naloga na Откажи", async () => {
    const { deleteAccount } = await import("../../api/authApi");
    await renderEditProfile();

    await act(async () => {
      fireEvent.click(screen.getByText("Обришите налог"));
    });

    await act(async () => {
      fireEvent.click(screen.getByText("Откажи"));
    });

    expect(deleteAccount).not.toHaveBeenCalled();
  });
});
