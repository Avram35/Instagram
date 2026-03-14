import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import Login from "./Login";
import { AppContext } from "../../context/AppContext";

const mockLogin = vi.fn();
const mockRegister = vi.fn();

const renderLogin = () => {
  render(
    <AppContext.Provider value={{ login: mockLogin, register: mockRegister }}>
      <Login />
    </AppContext.Provider>,
  );
};

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

describe("Login forma", () => {
  it("prikazuje login formu po defaultu", () => {
    renderLogin();
    expect(
      screen.getByPlaceholderText("Корисничко име или е-адреса"),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Лозинка")).toBeInTheDocument();
    expect(screen.getByText("Пријави се")).toBeInTheDocument();
  });

  it("prikazuje gresku ako su polja prazna", async () => {
    renderLogin();
    fireEvent.click(screen.getByText("Пријави се"));
    expect(
      await screen.findByText("Сва поља су обавезна!"),
    ).toBeInTheDocument();
  });

  it("poziva login sa ispravnim podacima", async () => {
    mockLogin.mockResolvedValue({ success: true });
    renderLogin();

    fireEvent.change(
      screen.getByPlaceholderText("Корисничко име или е-адреса"),
      {
        target: { value: "testuser" },
      },
    );
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByText("Пријави се"));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith("testuser", "password123");
    });
  });

  it("prikazuje gresku ako login nije uspesan", async () => {
    mockLogin.mockResolvedValue({
      success: false,
      message: "Погрешна лозинка!",
    });
    renderLogin();

    fireEvent.change(
      screen.getByPlaceholderText("Корисничко име или е-адреса"),
      {
        target: { value: "testuser" },
      },
    );
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "wrongpass" },
    });
    fireEvent.click(screen.getByText("Пријави се"));

    expect(await screen.findByText("Погрешна лозинка!")).toBeInTheDocument();
  });

  it("prikazuje dugme za prikaz lozinke kad se unese tekst", () => {
    renderLogin();
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "abc" },
    });
    expect(screen.getByText("Прикажи")).toBeInTheDocument();
  });

  it("prikazuje/sakriva lozinku", () => {
    renderLogin();
    const input = screen.getByPlaceholderText("Лозинка");
    fireEvent.change(input, { target: { value: "abc" } });

    fireEvent.click(screen.getByText("Прикажи"));
    expect(input).toHaveAttribute("type", "text");

    fireEvent.click(screen.getByText("Сакриј"));
    expect(input).toHaveAttribute("type", "password");
  });
});

describe("Registracija forma", () => {
  it("prebacuje na registraciju klikom na 'Региструјте се'", () => {
    renderLogin();
    fireEvent.click(screen.getAllByText("Региструјте се")[0]);
    expect(screen.getByPlaceholderText("Име")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Презиме")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Е-адреса")).toBeInTheDocument();
  });

  it("prikazuje gresku ako lozinke ne odgovaraju", async () => {
    renderLogin();
    fireEvent.click(screen.getAllByText("Региструјте се")[0]);

    fireEvent.change(screen.getByPlaceholderText("Име"), {
      target: { value: "Mihajlo" },
    });
    fireEvent.change(screen.getByPlaceholderText("Презиме"), {
      target: { value: "Timotijevic" },
    });
    fireEvent.change(screen.getByPlaceholderText("Корисничко име"), {
      target: { value: "mihajlotim" },
    });
    fireEvent.change(screen.getByPlaceholderText("Е-адреса"), {
      target: { value: "mihajlo@gmail.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "Lozinka1!" },
    });
    fireEvent.change(screen.getByPlaceholderText("Потврда лозинке"), {
      target: { value: "pass456" },
    });

    fireEvent.click(screen.getByText("Региструј се"));
    expect(
      await screen.findByText("Лозинке се не подударају!"),
    ).toBeInTheDocument();
  });

  it("prikazuje gresku ako je lozinka kraca od 6 karaktera", async () => {
    renderLogin();
    fireEvent.click(screen.getAllByText("Региструјте се")[0]);

    fireEvent.change(screen.getByPlaceholderText("Име"), {
      target: { value: "Mihajlo" },
    });
    fireEvent.change(screen.getByPlaceholderText("Презиме"), {
      target: { value: "Timotijevic" },
    });
    fireEvent.change(screen.getByPlaceholderText("Корисничко име"), {
      target: { value: "mihajlotim" },
    });
    fireEvent.change(screen.getByPlaceholderText("Е-адреса"), {
      target: { value: "mihajlo@gmail.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "abc" },
    });
    fireEvent.change(screen.getByPlaceholderText("Потврда лозинке"), {
      target: { value: "abc" },
    });

    fireEvent.click(screen.getByText("Региструј се"));
    expect(
      await screen.findByText("Лозинка мора имати минимум 6 карактера!"),
    ).toBeInTheDocument();
  });

  it("poziva register sa ispravnim podacima", async () => {
    mockRegister.mockResolvedValue({ success: true });
    renderLogin();
    fireEvent.click(screen.getAllByText("Региструјте се")[0]);

    fireEvent.change(screen.getByPlaceholderText("Име"), {
      target: { value: "Mihajlo" },
    });
    fireEvent.change(screen.getByPlaceholderText("Презиме"), {
      target: { value: "Timotijevic" },
    });
    fireEvent.change(screen.getByPlaceholderText("Корисничко име"), {
      target: { value: "mihajlotim" },
    });
    fireEvent.change(screen.getByPlaceholderText("Е-адреса"), {
      target: { value: "mihajlo@gmail.com" },
    });
    fireEvent.change(screen.getByPlaceholderText("Лозинка"), {
      target: { value: "Lozinka1!" },
    });
    fireEvent.change(screen.getByPlaceholderText("Потврда лозинке"), {
      target: { value: "Lozinka1!" },
    });

    fireEvent.click(screen.getByText("Региструј се"));

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith(
        "Mihajlo",
        "Timotijevic",
        "mihajlotim",
        "mihajlo@gmail.com",
        "Lozinka1!",
      );
    });
  });
});
