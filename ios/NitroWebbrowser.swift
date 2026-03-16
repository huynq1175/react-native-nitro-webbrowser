import Foundation
import SafariServices
import AuthenticationServices
import UIKit
import NitroModules

class NitroWebbrowser: HybridNitroWebbrowserSpec {
  private var safariVC: SFSafariViewController?
  private var webAuthSession: ASWebAuthenticationSession?
  private var redirectPromise: ((BrowserResult) -> Void)?
  private var redirectReject: ((Error) -> Void)?
  private var modalEnabled: Bool = false
  private var animated: Bool = false

  // Helper to get the NSObject-based delegate wrapper
  private lazy var delegateWrapper = NitroWebbrowserDelegateWrapper(owner: self)

  func open(url: String, options: InAppBrowserOptions) throws -> Promise<BrowserResult> {
    return Promise.async { [weak self] in
      guard let self = self else {
        throw NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "Instance deallocated"])
      }

      return try await withCheckedThrowingContinuation { continuation in
        DispatchQueue.main.async {
          if self.redirectPromise != nil {
            continuation.resume(returning: BrowserResult(type: "cancel", message: nil, url: nil))
            return
          }

          self.redirectPromise = { result in
            continuation.resume(returning: result)
          }
          self.redirectReject = { error in
            continuation.resume(throwing: error)
          }

          let dismissButtonStyle = options.dismissButtonStyle
          let preferredBarTintColor = options.preferredBarTintColor
          let preferredControlTintColor = options.preferredControlTintColor
          let modalPresentationStyle = options.modalPresentationStyle
          let modalTransitionStyle = options.modalTransitionStyle
          let formSheetPreferredContentSize = options.formSheetPreferredContentSize
          let readerMode = options.readerMode ?? false
          let enableBarCollapsing = options.enableBarCollapsing ?? false
          self.modalEnabled = options.modalEnabled ?? false
          self.animated = options.animated ?? true

          guard let browserURL = URL(string: url) else {
            self.redirectReject?(NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"]))
            self.flowDidFinish()
            return
          }

          let config = SFSafariViewController.Configuration()
          config.barCollapsingEnabled = enableBarCollapsing
          config.entersReaderIfAvailable = readerMode
          let safari = SFSafariViewController(url: browserURL, configuration: config)
          self.safariVC = safari
          safari.delegate = self.delegateWrapper

          if let style = dismissButtonStyle {
            switch style {
            case .done:
              safari.dismissButtonStyle = .done
            case .close:
              safari.dismissButtonStyle = .close
            case .cancel:
              safari.dismissButtonStyle = .cancel
            }
          }

          if let barTintColor = preferredBarTintColor {
            safari.preferredBarTintColor = self.colorFromHexString(barTintColor)
          }
          if let controlTintColor = preferredControlTintColor {
            safari.preferredControlTintColor = self.colorFromHexString(controlTintColor)
          }

          guard let ctrl = self.topViewController() else {
            self.redirectReject?(NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "No presenting view controller"]))
            self.flowDidFinish()
            return
          }

          if self.modalEnabled {
            let navController = UINavigationController(rootViewController: safari)
            navController.isNavigationBarHidden = true

            safari.modalPresentationStyle = .overFullScreen
            navController.modalPresentationStyle = self.getPresentationStyle(modalPresentationStyle)

            if self.animated, let transStyle = modalTransitionStyle {
              navController.modalTransitionStyle = self.getTransitionStyle(transStyle)
            }

            if modalPresentationStyle == .formsheet,
               let size = formSheetPreferredContentSize {
              navController.preferredContentSize = CGSize(width: size.width, height: size.height)
            }

            navController.presentationController?.delegate = self.delegateWrapper
            navController.isModalInPresentation = true

            ctrl.present(navController, animated: self.animated, completion: nil)
          } else {
            ctrl.present(safari, animated: self.animated, completion: nil)
          }
        }
      }
    }
  }

  func close() throws {
    DispatchQueue.main.async { [weak self] in
      guard let self = self else { return }
      guard let ctrl = self.topViewController() else { return }
      ctrl.dismiss(animated: self.animated) {
        self.redirectPromise?(BrowserResult(type: "dismiss", message: nil, url: nil))
        self.flowDidFinish()
      }
    }
  }

  func openAuth(url: String, redirectUrl: String, options: InAppBrowserOptions) throws -> Promise<BrowserResult> {
    return Promise.async { [weak self] in
      guard let self = self else {
        throw NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "Instance deallocated"])
      }

      return try await withCheckedThrowingContinuation { continuation in
        DispatchQueue.main.async {
          if self.redirectPromise != nil {
            continuation.resume(throwing: NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "Another browser is already open"]))
            return
          }

          self.redirectPromise = { result in
            continuation.resume(returning: result)
          }

          let ephemeralWebSession = options.ephemeralWebSession ?? false

          guard let authURL = URL(string: url) else {
            continuation.resume(throwing: NSError(domain: "NitroWebbrowser", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid URL"]))
            self.flowDidFinish()
            return
          }

          let callbackURLScheme = URL(string: redirectUrl)?.scheme

          let session = ASWebAuthenticationSession(
            url: authURL,
            callbackURLScheme: callbackURLScheme
          ) { [weak self] callbackURL, error in
            guard let self = self else { return }
            if let callbackURL = callbackURL {
              self.redirectPromise?(BrowserResult(
                type: "success",
                message: nil,
                url: callbackURL.absoluteString
              ))
            } else {
              self.redirectPromise?(BrowserResult(
                type: "cancel",
                message: error?.localizedDescription,
                url: nil
              ))
            }
            self.flowDidFinish()
          }

          self.webAuthSession = session

          if ephemeralWebSession {
            session.prefersEphemeralWebBrowserSession = true
          }
          session.presentationContextProvider = self.delegateWrapper

          session.start()
        }
      }
    }
  }

  func closeAuth() throws {
    DispatchQueue.main.async { [weak self] in
      guard let self = self else { return }
      self.redirectPromise?(BrowserResult(type: "dismiss", message: nil, url: nil))
      self.flowDidFinish()
      self.webAuthSession?.cancel()
    }
  }

  func isAvailable() throws -> Promise<Bool> {
    return Promise.resolved(withResult: true)
  }

  func warmup() throws -> Promise<Bool> {
    return Promise.resolved(withResult: false)
  }

  func mayLaunchUrl(mostLikelyUrl: String, otherUrls: [String]) throws {
    // No-op on iOS
  }

  // MARK: - Internal callbacks for delegate wrapper

  func handleSafariDidFinish() {
    redirectPromise?(BrowserResult(type: "cancel", message: nil, url: nil))
    flowDidFinish()
  }

  func handlePresentationDismiss() {
    redirectPromise?(BrowserResult(type: "cancel", message: nil, url: nil))
    flowDidFinish()
  }

  // MARK: - Private Helpers

  private func flowDidFinish() {
    safariVC = nil
    webAuthSession = nil
    redirectPromise = nil
    redirectReject = nil
  }

  private func topViewController() -> UIViewController? {
    var topController = UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first(where: { $0.isKeyWindow })?
      .rootViewController

    while let presented = topController?.presentedViewController {
      topController = presented
    }
    return topController
  }

  private func colorFromHexString(_ hex: String) -> UIColor? {
    var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
    hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

    var rgb: UInt64 = 0
    Scanner(string: hexSanitized).scanHexInt64(&rgb)

    let length = hexSanitized.count
    if length == 6 {
      return UIColor(
        red: CGFloat((rgb & 0xFF0000) >> 16) / 255.0,
        green: CGFloat((rgb & 0x00FF00) >> 8) / 255.0,
        blue: CGFloat(rgb & 0x0000FF) / 255.0,
        alpha: 1.0
      )
    } else if length == 8 {
      return UIColor(
        red: CGFloat((rgb & 0xFF000000) >> 24) / 255.0,
        green: CGFloat((rgb & 0x00FF0000) >> 16) / 255.0,
        blue: CGFloat((rgb & 0x0000FF00) >> 8) / 255.0,
        alpha: CGFloat(rgb & 0x000000FF) / 255.0
      )
    }
    return nil
  }

  private func getPresentationStyle(_ style: ModalPresentationStyle?) -> UIModalPresentationStyle {
    guard let style = style else { return .automatic }
    switch style {
    case .none: return .none
    case .fullscreen: return .fullScreen
    case .pagesheet: return .pageSheet
    case .formsheet: return .formSheet
    case .currentcontext: return .currentContext
    case .custom: return .custom
    case .overfullscreen: return .overFullScreen
    case .overcurrentcontext: return .overCurrentContext
    case .popover: return .popover
    case .automatic: return .automatic
    }
  }

  private func getTransitionStyle(_ style: ModalTransitionStyle) -> UIModalTransitionStyle {
    switch style {
    case .coververtical: return .coverVertical
    case .fliphorizontal: return .flipHorizontal
    case .crossdissolve: return .crossDissolve
    case .partialcurl: return .partialCurl
    }
  }
}

// MARK: - NSObject-based delegate wrapper for SFSafariViewController, ASWebAuthenticationSession, and UIAdaptivePresentationController
class NitroWebbrowserDelegateWrapper: NSObject, SFSafariViewControllerDelegate, ASWebAuthenticationPresentationContextProviding, UIAdaptivePresentationControllerDelegate {
  private weak var owner: NitroWebbrowser?

  init(owner: NitroWebbrowser) {
    self.owner = owner
    super.init()
  }

  func safariViewControllerDidFinish(_ controller: SFSafariViewController) {
    owner?.handleSafariDidFinish()
  }

  func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
    return UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first(where: { $0.isKeyWindow }) ?? ASPresentationAnchor()
  }

  func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
    owner?.handlePresentationDismiss()
  }
}
