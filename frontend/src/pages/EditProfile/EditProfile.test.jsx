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

const mockUser = {
  id: 1,
  username: "mihajlotim",
  fname: "Mihajlo",
  lname: "Timotijevic",
  bio: "Ja sam Mihajlo i vozim biciklu.",
  privateProfile: false,
};

const mockUpdateUser = vi.fn();

const renderEditProfile = async (user = mockUser) => {
  await act(async () => {
    render(
      <MemoryRouter>
        <AppContext.Provider value={{ user, updateUser: mockUpdateUser }}>
          <EditProfile />
        </AppContext.Provider>
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve({}),
    }),
  );
  localStorage.setItem("token", "test-token");
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
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
    fireEvent.change(fnameInput, { target: { value: "Novо ime" } });
    expect(fnameInput).toHaveValue("Novо ime");
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

  it("poziva fetch pri submit", async () => {
    await renderEditProfile();
    await act(async () => {
      fireEvent.click(screen.getByText("Пошаљи"));
    });
    expect(global.fetch).toHaveBeenCalled();
  });
});
